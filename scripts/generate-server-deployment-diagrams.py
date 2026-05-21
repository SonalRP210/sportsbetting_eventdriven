#!/usr/bin/env python3
"""
Generate two JPG comparison diagrams for sportsbetting on a rack server host.

  python scripts/generate-server-deployment-diagrams.py

Outputs (repo root by default):
  docs/architecture/sportsbetting-server-location-mapping.jpg
  docs/architecture/sportsbetting-hierarchy-and-cloud.jpg
"""

from pathlib import Path

import matplotlib.pyplot as plt
from matplotlib.patches import Circle, FancyArrowPatch, FancyBboxPatch, Rectangle

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "docs" / "architecture"
OUT_DIR.mkdir(parents=True, exist_ok=True)
BG = "#f4f6f8"


def save(fig, name: str) -> Path:
    path = OUT_DIR / name
    fig.savefig(path, format="jpeg", dpi=170, bbox_inches="tight", facecolor=BG)
    plt.close(fig)
    print(path)
    return path


def draw_server_schematic(ax, ox=0.05, oy=0.08, w=0.42, h=0.88):
    """Simple top-view server chassis with labeled zones."""
    x, y = ox, oy
    ax.add_patch(Rectangle((x, y), w, h, fill=False, edgecolor="#2c3e50", linewidth=3, transform=ax.transAxes))
    # Front (drives)
    ax.add_patch(Rectangle((x + 0.02, y + 0.02), w * 0.45, h * 0.22, facecolor="#aed6f1", edgecolor="#2471a3", transform=ax.transAxes))
    ax.text(x + w * 0.24, y + h * 0.12, "Front: HDD/SSD\nHot-swap bays", ha="center", fontsize=7, transform=ax.transAxes)
    # Fans
    ax.add_patch(Rectangle((x + w * 0.5, y + 0.02), w * 0.12, h * 0.22, facecolor="#d5d8dc", edgecolor="#566573", transform=ax.transAxes))
    ax.text(x + w * 0.56, y + h * 0.12, "Fans", ha="center", fontsize=7, transform=ax.transAxes)
    # CPU
    ax.add_patch(Rectangle((x + 0.02, y + h * 0.28), w * 0.28, h * 0.28, facecolor="#fdebd0", edgecolor="#d35400", transform=ax.transAxes))
    ax.text(x + w * 0.16, y + h * 0.42, "CPU +\nHeatsinks", ha="center", fontsize=7, weight="bold", transform=ax.transAxes)
    # RAM
    ax.add_patch(Rectangle((x + w * 0.32, y + h * 0.28), w * 0.28, h * 0.28, facecolor="#abebc6", edgecolor="#1e8449", transform=ax.transAxes))
    ax.text(x + w * 0.46, y + h * 0.42, "RAM\n(DIMM)", ha="center", fontsize=7, weight="bold", transform=ax.transAxes)
    # Motherboard base
    ax.add_patch(Rectangle((x + 0.02, y + h * 0.26), w * 0.6, h * 0.32, fill=False, edgecolor="#27ae60", linewidth=2, linestyle="--", transform=ax.transAxes))
    ax.text(x + w * 0.32, y + h * 0.58, "Motherboard", ha="center", fontsize=7, color="#1e8449", transform=ax.transAxes)
    # Rear PSU + NIC
    ax.add_patch(Rectangle((x + w * 0.62, y + h * 0.55), w * 0.34, h * 0.38, facecolor="#fadbd8", edgecolor="#c0392b", transform=ax.transAxes))
    ax.text(x + w * 0.79, y + h * 0.78, "PSU", ha="center", fontsize=7, transform=ax.transAxes)
    ax.text(x + w * 0.79, y + h * 0.62, "NIC / PCIe\nNetwork ports", ha="center", fontsize=7, transform=ax.transAxes)
    ax.text(x + w * 0.5, y + h * 0.96, "Case / chassis (worker node)", ha="center", fontsize=9, weight="bold", transform=ax.transAxes)


def image1_location_mapping():
    fig = plt.figure(figsize=(20, 14))
    fig.patch.set_facecolor(BG)
    ax = fig.add_axes([0, 0, 1, 1])
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")
    ax.text(0.5, 0.98, "Image 1 — Server location → hardware → sportsbetting (single-node K8s on host)",
            ha="center", fontsize=14, weight="bold")

    draw_server_schematic(ax, ox=0.03, oy=0.06, w=0.4, h=0.5)

    rows = [
        ("Whole silver enclosure", "Case/chassis", "One worker node; pods in VM/runtime"),
        ("Back right — Power Supply", "PSU", "Powers board + drives + fans only"),
        ("Large green board — Motherboard", "Motherboard", "CPU, RAM, PCIe, disk controllers"),
        ("Center — CPU / Heatsinks", "CPU + cooler", "kubelet, container runtime, JVMs odds:8080 betting:8084"),
        ("Right — RAM (Memory)", "RAM DIMM slots", "JVM heaps, PG shared_buffers, Kafka page cache"),
        ("Back — Expansion Cards", "PCIe NIC (opt.)", "Extra NIC, teaming, cluster traffic"),
        ("Back — Network Ports", "NIC", "Ingress, gateway, Kafka, JDBC"),
        ("Front left — Hard Drives / SSDs", "Drive cage + disks", "OS, images, PG odds/betting schemas, Kafka logs"),
        ("Front right — Hot-Swap Bays", "Drive cage", "Hot-swap volumes / RAID"),
        ("Middle — Cooling Fans", "Fan modules", "Cooling only"),
        ("Back right small fans", "Fan modules", "Exhaust near PSU"),
        ("PSU cable bundles", "Cabling (power)", "PSU → board, drives, fans"),
        ("Front ↔ board cables", "Cabling (data)", "SATA/SAS → /dev/sd*"),
        ("USB & Management Ports", "(extra)", "iDRAC/IPMI — ops, not app traffic"),
    ]

    y0 = 0.62
    rh = 0.023
    ax.add_patch(Rectangle((0.48, 0.05), 0.5, 0.55, facecolor="white", edgecolor="#bdc3c7", transform=ax.transAxes))
    ax.text(0.48 + 0.25, 0.58, "Mapping table", ha="center", fontsize=11, weight="bold", transform=ax.transAxes)
    headers = ("Location on image", "Hardware", "Sportsbetting on host")
    ax.text(0.49, 0.555, f"{headers[0]:<22} | {headers[1]:<18} | {headers[2]}", fontsize=6.5, family="monospace", transform=ax.transAxes)
    for i, (loc, hw, app) in enumerate(rows):
        yy = 0.535 - i * rh
        line = f"{loc[:28]:<28} | {hw[:18]:<18} | {app[:42]}"
        ax.text(0.49, yy, line, fontsize=5.8, family="monospace", transform=ax.transAxes, va="top")

    # Callout arrows from schematic
    callouts = [
        (0.16, 0.35, 0.48, 0.52, "#3498db"),
        (0.28, 0.35, 0.48, 0.48, "#27ae60"),
        (0.35, 0.35, 0.48, 0.44, "#1e8449"),
        (0.12, 0.18, 0.48, 0.38, "#2471a3"),
        (0.32, 0.55, 0.48, 0.32, "#c0392b"),
    ]
    for x1, y1, x2, y2, c in callouts:
        ax.add_patch(FancyArrowPatch((x1, y1), (x2, y2), arrowstyle="-|>", color=c, linewidth=1.2,
                                     mutation_scale=10, transform=ax.transAxes))

    return save(fig, "sportsbetting-server-location-mapping.jpg")


def image2_hierarchy_and_cloud():
    fig = plt.figure(figsize=(20, 14))
    fig.patch.set_facecolor(BG)
    ax = fig.add_axes([0, 0, 1, 1])
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")
    ax.text(0.5, 0.98, "Image 2 — Visual flow, hierarchy match, cloud vs single chassis",
            ha="center", fontsize=14, weight="bold")

    # Visual flow (top)
    ax.add_patch(FancyBboxPatch((0.03, 0.72), 0.94, 0.2, boxstyle="round,pad=0.01", facecolor="#fcf3cf",
                                edgecolor="#f39c12", linewidth=2, transform=ax.transAxes))
    flow = (
        "[ Hot-swap + HDDs ]  →  Postgres files, Kafka log segments\n"
        "[ Cooling fans ]     →  airflow only (no software)\n"
        "[ CPU×2 + heatsinks ] →  odds + betting JVMs (kubelet, container runtime)\n"
        "[ RAM×12 ]           →  JVM heap + DB cache (shared_buffers, page cache)\n"
        "[ Motherboard ]      →  platform\n"
        "[ Expansion / Network Ports ] ← clients, gateway, Kafka, JDBC\n"
        "[ Power Supply ]     →  power only"
    )
    ax.text(0.5, 0.82, flow, ha="center", va="center", fontsize=9, family="monospace", transform=ax.transAxes)

    # Hierarchy table (middle left)
    ax.add_patch(Rectangle((0.03, 0.28), 0.46, 0.4, facecolor="white", edgecolor="#bdc3c7", transform=ax.transAxes))
    ax.text(0.26, 0.66, "Hierarchy vs labeled server image", ha="center", fontsize=11, weight="bold", transform=ax.transAxes)
    hier = [
        ("Case/chassis", "✅ Whole unit", "Cloud worker / bare-metal node"),
        ("PSU ×1 or ×2", "✅ Power Supply", "Infrastructure only"),
        ("Motherboard", "✅ Labeled", "Connects all parts"),
        ("├── CPU + cooler", "✅ CPU / Heatsinks", "Compute: pods / JVMs"),
        ("├── RAM DIMM", "✅ RAM (Memory)", "Working set apps + DB"),
        ("└── PCIe NIC", "✅ Expansion + Network", "HTTP / Kafka / JDBC"),
        ("Drives + cables", "✅ Front bays + HDD", "PG, Kafka, images (if on-node)"),
        ("Fan modules", "✅ Cooling Fans", "Not app architecture"),
        ("Cabling", "✅ Harnesses", "Physical only"),
    ]
    for i, (h, m, p) in enumerate(hier):
        ax.text(0.04, 0.62 - i * 0.038, f"{h:<22} {m:<22} {p}", fontsize=7.5, family="monospace", transform=ax.transAxes)

    # Cloud comparison (middle right)
    ax.add_patch(Rectangle((0.52, 0.28), 0.45, 0.4, facecolor="white", edgecolor="#bdc3c7", transform=ax.transAxes))
    ax.text(0.745, 0.66, "Cloud vs this single chassis", ha="center", fontsize=11, weight="bold", transform=ax.transAxes)
    cloud = [
        ("Single VM on this host", "CPU+RAM = JVMs; front drives = DB + maybe Kafka"),
        ("EKS/GKE/AKS + RDS + MSK", "Host = pods only; drives = node OS; PG/Kafka external"),
        ("Monorepo logical view", "Distributed by process + Kafka even on one server"),
    ]
    colors = ["#d4e6f1", "#fadbd8", "#d5f5e3"]
    for i, (style, where) in enumerate(cloud):
        yy = 0.55 - i * 0.1
        ax.add_patch(Rectangle((0.54, yy - 0.03), 0.41, 0.08, facecolor=colors[i], edgecolor="#7f8c8d", transform=ax.transAxes))
        ax.text(0.745, yy + 0.01, f"{style}\n{where}", ha="center", va="center", fontsize=8, transform=ax.transAxes)

    # Mini schematic bottom
    draw_server_schematic(ax, ox=0.28, oy=0.02, w=0.44, h=0.22)
    ax.text(0.5, 0.26, "odds-service + betting-service pods share CPU/RAM; data on disks or managed cloud DB",
            ha="center", fontsize=9, style="italic", transform=ax.transAxes)

    return save(fig, "sportsbetting-hierarchy-and-cloud.jpg")


def main():
    image1_location_mapping()
    image2_hierarchy_and_cloud()


if __name__ == "__main__":
    main()
