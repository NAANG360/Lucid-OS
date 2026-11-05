#!/usr/bin/env python3
"""Serialize a fully reconstructed ext4 image back into Android block-OTA new.dat order."""
import os, sys
BLOCK=4096
if len(sys.argv) != 4:
    raise SystemExit("usage: repack_transfer_image.py TRANSFER_LIST IMAGE OUTPUT_NEW_DAT")
transfer, image_path, output_path=sys.argv[1:]
lines=[line.strip() for line in open(transfer,encoding="utf-8") if line.strip()]
if len(lines)<4: raise SystemExit("transfer list is truncated")
version,total,stash,max_stash=map(int,lines[:4])
if version != 4: raise SystemExit(f"expected transfer-list v4, got {version}")
commands=[]; ranges_all=[]; new_blocks=zero_blocks=erase_blocks=0
for no,line in enumerate(lines[4:],5):
    cmd,sep,arg=line.partition(" ")
    if not sep or cmd not in ("new","zero","erase"):
        raise SystemExit(f"unsupported command on line {no}: {line}")
    fields=[int(x) for x in arg.split(",") if x]
    if not fields or fields[0] != len(fields[1:]) or len(fields[1:])%2:
        raise SystemExit(f"bad range command line {no}")
    pairs=list(zip(fields[1::2],fields[2::2]))
    count=sum(end-start for start,end in pairs)
    if cmd=="new": new_blocks+=count
    elif cmd=="zero": zero_blocks+=count
    else: erase_blocks+=count
    commands.append((cmd,pairs))
    ranges_all.extend((start,end,cmd) for start,end in pairs)
if zero_blocks or erase_blocks:
    raise SystemExit("Cannot serialize modified image safely: transfer list contains zero/erase operations.")
if new_blocks != total:
    raise SystemExit(f"transfer header says {total} blocks but new commands cover {new_blocks}")
ranges=sorted(ranges_all)
cursor=0
for start,end,cmd in ranges:
    if start != cursor or end < start:
        raise SystemExit(f"new ranges are not a gapless, non-overlapping full image at block {cursor}: {start}-{end}")
    cursor=end
size=os.path.getsize(image_path)
if size % BLOCK or cursor != size//BLOCK:
    raise SystemExit(f"image size/ranges disagree: image={size}, range-end={cursor} blocks")
os.makedirs(os.path.dirname(os.path.abspath(output_path)),exist_ok=True)
with open(image_path,"rb") as image, open(output_path,"wb") as out:
    for cmd,pairs in commands:
        if cmd!="new": continue
        for start,end in pairs:
            image.seek(start*BLOCK)
            remaining=(end-start)*BLOCK
            while remaining:
                data=image.read(min(4*1024*1024,remaining))
                if not data: raise SystemExit("image ended while serializing transfer-list ranges")
                out.write(data); remaining-=len(data)
print(f"serialized_blocks={new_blocks} bytes={os.path.getsize(output_path)}")
