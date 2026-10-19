/*
 * Native Qualcomm SDM660 / Xiaomi Redmi Note 7 (lavender) machine.
 *
 * Copyright (c) 2026
 * SPDX-License-Identifier: GPL-2.0-or-later
 *
 * This is an initial boot-platform implementation, not a claim of complete
 * Kryo 260 or Qualcomm firmware emulation. CPU execution uses QEMU ARM CPU
 * models; MPIDR affinity layout follows the pinned lavender SoC DTSI.
 */
#include "qemu/osdep.h"
#include "qemu/units.h"
#include "qapi/error.h"
#include "qemu/error-report.h"
#include "qobject/qlist.h"
#include "system/device_tree.h"
#include "system/system.h"
#include "exec/address-spaces.h"
#include "hw/arm/boot.h"
#include "hw/core/boards.h"
#include "hw/core/sysbus-fdt.h"
#include "hw/qdev-properties.h"
#include "hw/qdev-properties-system.h"
#include "hw/char/serial.h"
#include "hw/intc/arm_gicv3_common.h"
#include "hw/irq.h"
#include "hw/sysbus.h"
#include "target/arm/cpu.h"
#include "target/arm/cpu-qom.h"
#include "target/arm/gtimer.h"

#define TYPE_SDM660_LAVENDER_MACHINE MACHINE_TYPE_NAME("sdm660-lavender")
OBJECT_DECLARE_SIMPLE_TYPE(SDM660LavenderState, SDM660_LAVENDER)

#define SDM660_RAM_BASE       0x80000000ULL
#define SDM660_RAM_DEFAULT    (4 * GiB)
#define SDM660_GICD_BASE      0x17a00000ULL
#define SDM660_GICD_SIZE      0x10000
#define SDM660_GICR_BASE      0x17b00000ULL
#define SDM660_GICR_SIZE      0x100000
#define SDM660_UART_BASE      0x0c170000ULL
#define SDM660_UART_SIZE      0x1000
#define SDM660_UART_SPI       108
#define SDM660_NUM_IRQS       256
#define SDM660_TIMER_HZ       19200000

struct SDM660LavenderState {
    MachineState parent_obj;
    struct arm_boot_info bootinfo;
    DeviceState *gic;
    void *fdt;
    int fdt_size;
};

static uint64_t lavender_mpidr(unsigned int index)
{
    /* DTSI MPIDR affinity: 0..3 and 0x100..0x103. */
    return index < 4 ? index : 0x100 + index - 4;
}

static const CPUArchIdList *lavender_possible_cpu_arch_ids(MachineState *ms)
{
    unsigned int i, max = ms->smp.max_cpus;
    if (ms->possible_cpus) {
        return ms->possible_cpus;
    }
    ms->possible_cpus = g_malloc0(sizeof(CPUArchIdList) +
                                  sizeof(CPUArchId) * max);
    ms->possible_cpus->len = max;
    for (i = 0; i < max; i++) {
        CPUArchId *cpu = &ms->possible_cpus->cpus[i];
        cpu->type = (i < 4) ? ARM_CPU_TYPE_NAME("cortex-a53")
                            : ARM_CPU_TYPE_NAME("cortex-a72");
        cpu->arch_id = lavender_mpidr(i);
        cpu->props.has_thread_id = true;
        cpu->props.thread_id = i;
    }
    return ms->possible_cpus;
}

static void lavender_add_cpu_nodes(SDM660LavenderState *s)
{
    MachineState *ms = MACHINE(s);
    char name[64];
    unsigned int i;
    qemu_fdt_add_subnode(s->fdt, "/cpus");
    qemu_fdt_setprop_cell(s->fdt, "/cpus", "#address-cells", 2);
    qemu_fdt_setprop_cell(s->fdt, "/cpus", "#size-cells", 0);
    qemu_fdt_setprop_cell(s->fdt, "/cpus", "timebase-frequency",
                           SDM660_TIMER_HZ);
    for (i = 0; i < ms->smp.cpus; i++) {
        snprintf(name, sizeof(name), "/cpus/cpu@%llx",
                 (unsigned long long)lavender_mpidr(i));
        qemu_fdt_add_subnode(s->fdt, name);
        qemu_fdt_setprop_string(s->fdt, name, "device_type", "cpu");
        qemu_fdt_setprop_string(s->fdt, name, "compatible",
                                i < 4 ? "arm,cortex-a53" : "arm,cortex-a72");
        qemu_fdt_setprop_sized_cells(s->fdt, name, "reg", 2,
                                     lavender_mpidr(i));
        if (i >= ms->smp.cpus) {
            qemu_fdt_setprop_string(s->fdt, name, "status", "disabled");
        }
    }
}

static void lavender_create_fdt(SDM660LavenderState *s)
{
    char *fdt;
    s->fdt = create_device_tree(&s->fdt_size);
    if (!s->fdt) {
        error_report("sdm660-lavender: could not allocate guest device tree");
        exit(1);
    }
    fdt = s->fdt;
    qemu_fdt_setprop_string(fdt, "/", "compatible",
                            "xiaomi,lavender-sdm660");
    qemu_fdt_setprop_cell(fdt, "/", "#address-cells", 2);
    qemu_fdt_setprop_cell(fdt, "/", "#size-cells", 2);
    qemu_fdt_add_subnode(fdt, "/memory");
    qemu_fdt_setprop_string(fdt, "/memory", "device_type", "memory");
    qemu_fdt_setprop_sized_cells(fdt, "/memory", "reg", 2,
                                 SDM660_RAM_BASE, 2, MACHINE(s)->ram_size);
    lavender_add_cpu_nodes(s);

    qemu_fdt_add_subnode(fdt, "/timer");
    qemu_fdt_setprop_string(fdt, "/timer", "compatible", "arm,armv8-timer");
    qemu_fdt_setprop_cell(fdt, "/timer", "interrupt-parent", 1);
    qemu_fdt_setprop(fdt, "/timer", "always-on", NULL, 0);
    qemu_fdt_setprop_cells(fdt, "/timer", "interrupts",
                           1, 13, 4, 1, 14, 4, 1, 11, 4, 1, 10, 4);

    qemu_fdt_add_subnode(fdt, "/intc");
    qemu_fdt_setprop_string(fdt, "/intc", "compatible", "arm,gic-v3");
    qemu_fdt_setprop(fdt, "/intc", "interrupt-controller", NULL, 0);
    qemu_fdt_setprop_cell(fdt, "/intc", "#interrupt-cells", 3);
    qemu_fdt_setprop_cell(fdt, "/intc", "#redistributor-regions", 1);
    qemu_fdt_setprop_cell(fdt, "/intc", "#address-cells", 2);
    qemu_fdt_setprop(fdt, "/intc", "ranges", NULL, 0);
    qemu_fdt_setprop_sized_cells(fdt, "/intc", "reg",
                                 2, SDM660_GICD_BASE, 2, SDM660_GICD_SIZE,
                                 2, SDM660_GICR_BASE, 2, SDM660_GICR_SIZE);
    qemu_fdt_setprop_cell(fdt, "/intc", "phandle", 1);

    qemu_fdt_add_subnode(fdt, "/soc");
    qemu_fdt_setprop_cell(fdt, "/soc", "#address-cells", 2);
    qemu_fdt_setprop_cell(fdt, "/soc", "#size-cells", 2);
    qemu_fdt_setprop(fdt, "/soc", "ranges", NULL, 0);
    qemu_fdt_add_subnode(fdt, "/soc/serial@c170000");
    qemu_fdt_setprop_string(fdt, "/soc/serial@c170000", "compatible",
                            "qcom,msm-uartdm");
    qemu_fdt_setprop_sized_cells(fdt, "/soc/serial@c170000", "reg",
                                 2, SDM660_UART_BASE, 2, SDM660_UART_SIZE);
    qemu_fdt_setprop_string(fdt, "/soc/serial@c170000", "status", "okay");
    qemu_fdt_add_subnode(fdt, "/chosen");
    qemu_fdt_setprop_string(fdt, "/chosen", "stdout-path",
                            "/soc/serial@c170000");
}

static void *lavender_get_dtb(const struct arm_boot_info *binfo, int *size)
{
    SDM660LavenderState *s = container_of(binfo, SDM660LavenderState,
                                          bootinfo);
    *size = s->fdt_size;
    return s->fdt;
}

static void lavender_create_gic(SDM660LavenderState *s)
{
    MachineState *ms = MACHINE(s);
    SysBusDevice *gicbus;
    unsigned int i;
    QList *redist_region_count = qlist_new();
    s->gic = qdev_new(gicv3_class_name());
    qdev_prop_set_uint32(s->gic, "revision", 3);
    qdev_prop_set_uint32(s->gic, "num-cpu", ms->smp.cpus);
    qdev_prop_set_uint32(s->gic, "num-irq", SDM660_NUM_IRQS + 32);
    qdev_prop_set_bit(s->gic, "has-security-extensions", true);
    object_property_set_link(OBJECT(s->gic), "sysmem",
                             OBJECT(get_system_memory()), &error_fatal);
    qlist_append_int(redist_region_count, ms->smp.cpus);
    qdev_prop_set_array(s->gic, "redist-region-count", redist_region_count);
    gicbus = SYS_BUS_DEVICE(s->gic);
    sysbus_realize_and_unref(gicbus, &error_fatal);
    sysbus_mmio_map(gicbus, 0, SDM660_GICD_BASE);
    sysbus_mmio_map(gicbus, 1, SDM660_GICR_BASE);

    for (i = 0; i < ms->smp.cpus; i++) {
        DeviceState *cpu = DEVICE(qemu_get_cpu(i));
        const int timer_irq[] = {
            [GTIMER_PHYS] = ARCH_TIMER_NS_EL1_IRQ,
            [GTIMER_VIRT] = ARCH_TIMER_VIRT_IRQ,
            [GTIMER_HYP] = ARCH_TIMER_NS_EL2_IRQ,
            [GTIMER_SEC] = ARCH_TIMER_S_EL1_IRQ,
            [GTIMER_HYPVIRT] = ARCH_TIMER_NS_EL2_VIRT_IRQ,
        };
        int irq;
        for (irq = 0; irq < ARRAY_SIZE(timer_irq); irq++) {
            qdev_connect_gpio_out(cpu, irq,
                qdev_get_gpio_in(s->gic, SDM660_NUM_IRQS + i * GIC_INTERNAL
                                 + timer_irq[irq]));
        }
        sysbus_connect_irq(gicbus, i,
                           qdev_get_gpio_in(cpu, ARM_CPU_IRQ));
        sysbus_connect_irq(gicbus, i + ms->smp.cpus,
                           qdev_get_gpio_in(cpu, ARM_CPU_FIQ));
        sysbus_connect_irq(gicbus, i + 2 * ms->smp.cpus,
                           qdev_get_gpio_in(cpu, ARM_CPU_VIRQ));
        sysbus_connect_irq(gicbus, i + 3 * ms->smp.cpus,
                           qdev_get_gpio_in(cpu, ARM_CPU_VFIQ));
    }
}

static void lavender_init(MachineState *machine)
{
    SDM660LavenderState *s = SDM660_LAVENDER(machine);
    const CPUArchIdList *possible = lavender_possible_cpu_arch_ids(machine);
    unsigned int i;
    DeviceState *uart;
    SysBusDevice *uartbus;
    if (machine->ram_size > SDM660_RAM_DEFAULT) {
        error_report("sdm660-lavender: RAM exceeds 4 GiB board profile");
        exit(1);
    }
    memory_region_add_subregion(get_system_memory(), SDM660_RAM_BASE,
                                machine->ram);
    for (i = 0; i < machine->smp.cpus; i++) {
        Object *obj = object_new(possible->cpus[i].type);
        object_property_set_int(obj, "mp-affinity",
                                possible->cpus[i].arch_id, &error_abort);
        object_property_set_int(obj, "cntfrq", SDM660_TIMER_HZ, &error_abort);
        object_property_set_link(obj, "memory",
                                 OBJECT(get_system_memory()), &error_abort);
        CPU(obj)->cpu_index = i;
        qdev_realize(DEVICE(obj), NULL, &error_fatal);
        object_unref(obj);
    }
    lavender_create_gic(s);
    uart = qdev_new("sdm660-uartdm");
    qdev_prop_set_chr(uart, "chardev", serial_hd(0));
    uartbus = SYS_BUS_DEVICE(uart);
    sysbus_realize_and_unref(uartbus, &error_fatal);
    sysbus_mmio_map(uartbus, 0, SDM660_UART_BASE);
    sysbus_connect_irq(uartbus, 0,
                       qdev_get_gpio_in(s->gic, 32 + SDM660_UART_SPI));

    lavender_create_fdt(s);
    s->bootinfo.ram_size = machine->ram_size;
    s->bootinfo.board_id = -1;
    s->bootinfo.loader_start = SDM660_RAM_BASE;
    s->bootinfo.get_dtb = lavender_get_dtb;
    s->bootinfo.firmware_loaded = false;
    arm_load_kernel(ARM_CPU(first_cpu), machine, &s->bootinfo);
}

static void lavender_machine_class_init(ObjectClass *oc, void *data)
{
    MachineClass *mc = MACHINE_CLASS(oc);
    mc->desc = "Xiaomi Redmi Note 7 (lavender), Qualcomm SDM660";
    mc->init = lavender_init;
    mc->default_cpu_type = ARM_CPU_TYPE_NAME("cortex-a53");
    mc->default_ram_size = SDM660_RAM_DEFAULT;
    mc->max_cpus = 8;
    mc->default_cpus = 1;
    mc->possible_cpu_arch_ids = lavender_possible_cpu_arch_ids;
    machine_class_allow_dynamic_sysbus_dev(mc, "sdm660-uartdm");
    mc->ignore_memory_transaction_failures = false;
}

static const TypeInfo lavender_machine_info = {
    .name = TYPE_SDM660_LAVENDER_MACHINE,
    .parent = TYPE_MACHINE,
    .instance_size = sizeof(SDM660LavenderState),
    .class_init = lavender_machine_class_init,
};

static void lavender_machine_register_types(void)
{
    type_register_static(&lavender_machine_info);
}
type_init(lavender_machine_register_types)
