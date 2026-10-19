/*
 * Qualcomm UARTDM transmit-path model for the SDM660 lavender board.
 *
 * This is a QEMU SysBus device implementation scaffold, deliberately limited
 * to the polling transmit path used by early boot. It is not a complete UARTDM
 * implementation: RX, interrupts, DMA/BAM, clocks, and modem handshakes are
 * not modeled. Do not claim Android bootability from this device alone.
 *
 * Register offsets are based on public Qualcomm early-boot UARTDM code.
 */
#include "qemu/osdep.h"
#include "hw/sysbus.h"
#include "hw/qdev-properties.h"
#include "hw/qdev-properties-system.h"
#include "chardev/char-fe.h"
#include "qemu/log.h"
#include "qemu/module.h"
#include "qapi/error.h"
#include "exec/memory.h"

#define TYPE_SDM660_UARTDM "sdm660-uartdm"
OBJECT_DECLARE_SIMPLE_TYPE(SDM660UARTDMState, SDM660_UARTDM)

#define UARTDM_MMIO_SIZE       0x1000
#define UARTDM_DMEN            0x03c
#define UARTDM_SR              0x0a4
#define UARTDM_CR              0x0a8
#define UARTDM_TF              0x100
#define UARTDM_RF              0x140
#define UARTDM_CSR             0x0a0
#define UARTDM_IMR             0x0b0
#define UARTDM_TX_READY        (1u << 2)
#define UARTDM_RX_READY        (1u << 0)
#define UARTDM_RX_FIFO_SIZE    256
#define UARTDM_TX_BAM_ENABLE   (1u << 2)
#define UARTDM_TX_SINGLE_CHAR  (1u << 4)
#define UARTDM_CR_RESET_TX     (2u << 4)
#define UARTDM_CR_TX_ENABLE    (1u << 2)
#define UARTDM_CR_TX_DISABLE   (1u << 3)

struct SDM660UARTDMState {
    SysBusDevice parent_obj;
    MemoryRegion iomem;
    CharBackend chr;
    uint32_t dmen;
    uint32_t cr;
    uint32_t mr1;
    uint32_t mr2;
    uint32_t imr;
    uint32_t csr;
    uint32_t tf;
    uint64_t tx_bytes;
    bool tx_enabled;
    uint8_t rx_fifo[UARTDM_RX_FIFO_SIZE];
    unsigned rx_head;
    unsigned rx_count;
};

static bool sdm660_uartdm_can_receive(void *opaque)
{
    SDM660UARTDMState *s = SDM660_UARTDM(opaque);
    return s->rx_count < UARTDM_RX_FIFO_SIZE;
}

static void sdm660_uartdm_receive(void *opaque, const uint8_t *buf, int size)
{
    SDM660UARTDMState *s = SDM660_UARTDM(opaque);
    int i;

    for (i = 0; i < size && s->rx_count < UARTDM_RX_FIFO_SIZE; i++) {
        unsigned tail = (s->rx_head + s->rx_count) % UARTDM_RX_FIFO_SIZE;
        s->rx_fifo[tail] = buf[i];
        s->rx_count++;
    }
}

static uint64_t sdm660_uartdm_read(void *opaque, hwaddr offset,
                                   unsigned size)
{
    SDM660UARTDMState *s = SDM660_UARTDM(opaque);

    if (size != 4) {
        qemu_log_mask(LOG_GUEST_ERROR,
                      "sdm660-uartdm: unsupported %u-byte read at 0x%"
                      HWADDR_PRIx "\n", size, offset);
        return 0;
    }

    switch (offset) {
    case UARTDM_DMEN:
        return s->dmen;
    case UARTDM_SR:
        /* TX is immediate; RX-ready reflects bytes queued by the chardev. */
        return UARTDM_TX_READY | (s->rx_count ? UARTDM_RX_READY : 0);
    case UARTDM_CR:
        return s->cr;
    case 0x000: return s->mr1;
    case 0x004: return s->mr2;
    case UARTDM_CSR: return s->csr;
    case UARTDM_IMR: return s->imr;
    case UARTDM_RF:
        if (s->rx_count) {
            uint8_t byte = s->rx_fifo[s->rx_head];
            s->rx_head = (s->rx_head + 1) % UARTDM_RX_FIFO_SIZE;
            s->rx_count--;
            return byte;
        }
        return 0;
    default:
        return 0;
    }
}

static void sdm660_uartdm_write(void *opaque, hwaddr offset,
                                uint64_t value, unsigned size)
{
    SDM660UARTDMState *s = SDM660_UARTDM(opaque);
    uint32_t v = value;

    if (size != 4) {
        qemu_log_mask(LOG_GUEST_ERROR,
                      "sdm660-uartdm: unsupported %u-byte write at 0x%"
                      HWADDR_PRIx "\n", size, offset);
        return;
    }

    switch (offset) {
    case UARTDM_TF: {
        uint8_t byte = v & 0xff;
        if (s->tx_enabled && qemu_chr_fe_backend_connected(&s->chr)) {
            int ret = qemu_chr_fe_write_all(&s->chr, &byte, 1);
            if (ret < 0) {
                qemu_log_mask(LOG_GUEST_ERROR,
                              "sdm660-uartdm: chardev transmit failed: %d\n",
                              ret);
            }
        }
        s->tf = v;
        s->tx_bytes++;
        break;
    }
    case UARTDM_DMEN:
        /* Match the documented early-boot polling setup, not full hardware. */
        s->dmen = (v & ~UARTDM_TX_BAM_ENABLE) | UARTDM_TX_SINGLE_CHAR;
        break;
    case UARTDM_CR:
        s->cr = v;
        if (v == UARTDM_CR_RESET_TX) {
            s->tf = 0;
            s->tx_enabled = false;
        } else if (v == UARTDM_CR_TX_ENABLE) {
            s->tx_enabled = true;
        } else if (v == UARTDM_CR_TX_DISABLE) {
            s->tx_enabled = false;
        }
        break;
    case 0x000: s->mr1 = v; break;
    case 0x004: s->mr2 = v; break;
    case UARTDM_CSR: s->csr = v; break;
    case UARTDM_IMR: s->imr = v; break;
    default:
        /* Ignore unknown writes; log only to avoid drowning early boot. */
        break;
    }
}

static const MemoryRegionOps sdm660_uartdm_ops = {
    .read = sdm660_uartdm_read,
    .write = sdm660_uartdm_write,
    .endianness = DEVICE_LITTLE_ENDIAN,
    .valid = {
        .min_access_size = 4,
        .max_access_size = 4,
        .unaligned = false,
    },
    .impl = {
        .min_access_size = 4,
        .max_access_size = 4,
        .unaligned = false,
    },
};

static void sdm660_uartdm_reset(DeviceState *dev)
{
    SDM660UARTDMState *s = SDM660_UARTDM(dev);

    s->dmen = 0;
    s->cr = 0;
    s->mr1 = 0;
    s->mr2 = 0;
    s->imr = 0;
    s->csr = 0;
    s->tf = 0;
    s->tx_bytes = 0;
    s->tx_enabled = false;
    s->rx_head = 0;
    s->rx_count = 0;
}

static void sdm660_uartdm_realize(DeviceState *dev, Error **errp)
{
    SDM660UARTDMState *s = SDM660_UARTDM(dev);

    if (!qemu_chr_fe_backend_connected(&s->chr)) {
        error_setg(errp, "sdm660-uartdm requires a QEMU chardev backend");
        return;
    }
    qemu_chr_fe_set_handlers(&s->chr, sdm660_uartdm_can_receive,
                             sdm660_uartdm_receive, NULL, NULL, s, NULL, true);
}

static void sdm660_uartdm_init(Object *obj)
{
    SDM660UARTDMState *s = SDM660_UARTDM(obj);

    memory_region_init_io(&s->iomem, obj, &sdm660_uartdm_ops, s,
                          TYPE_SDM660_UARTDM, UARTDM_MMIO_SIZE);
    sysbus_init_mmio(SYS_BUS_DEVICE(obj), &s->iomem);
}

static const Property sdm660_uartdm_properties[] = {
    DEFINE_PROP_CHR("chardev", SDM660UARTDMState, chr),
};

static void sdm660_uartdm_class_init(ObjectClass *klass, void *data)
{
    DeviceClass *dc = DEVICE_CLASS(klass);

    dc->realize = sdm660_uartdm_realize;
    dc->user_creatable = true;
    device_class_set_legacy_reset(dc, sdm660_uartdm_reset);
    dc->vmsd = NULL;
    device_class_set_props(dc, sdm660_uartdm_properties);
}

static const TypeInfo sdm660_uartdm_info = {
    .name = TYPE_SDM660_UARTDM,
    .parent = TYPE_DYNAMIC_SYS_BUS_DEVICE,
    .instance_size = sizeof(SDM660UARTDMState),
    .instance_init = sdm660_uartdm_init,
    .class_init = sdm660_uartdm_class_init,
};

static void sdm660_uartdm_register_types(void)
{
    type_register_static(&sdm660_uartdm_info);
}

type_init(sdm660_uartdm_register_types)
