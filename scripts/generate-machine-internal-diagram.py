#!/usr/bin/env python3
"""
Generate a machine-internal stack diagram (JPG) locally for odds + betting on one K8s worker.

Does not run in CI; no image is committed to the repo.

Usage:
  pip install matplotlib pillow
  python scripts/generate-machine-internal-diagram.py

Output (current working directory):
  machine-internal-odds-betting.jpg
"""

from pathlib import Path

import matplotlib.pyplot as plt
from matplotlib.patches import FancyArrowPatch, FancyBboxPatch

OUT = Path("machine-internal-odds-betting.jpg")
BG = "#eef2f7"


def box(ax, x, y, w, h, title, lines, fc, ec):
    ax.add_patch(
        FancyBboxPatch(
            (x, y),
            w,
            h,
            boxstyle="round,pad=0.01,rounding_size=0.02",
            linewidth=2,
            edgecolor=ec,
            facecolor=fc,
            transform=ax.transAxes,
        )
    )
    ax.text(
        x + w / 2,
        y + h - 0.015,
        title,
        ha="center",
        va="top",
        fontsize=10,
        weight="bold",
        transform=ax.transAxes,
    )
    ax.text(
        x + w / 2,
        y + h / 2 - 0.02,
        "\n".join(lines),
        ha="center",
        va="center",
        fontsize=7.5,
        family="monospace",
        transform=ax.transAxes,
        linespacing=1.2,
    )


def main():
    fig, ax = plt.subplots(figsize=(16, 12))
    fig.patch.set_facecolor(BG)
    ax.set_xlim(0, 1)
    ax.set_ylim(0, 1)
    ax.axis("off")
    ax.text(
        0.5,
        0.98,
        "Cloud VM / K8s worker — internal view (odds + betting)",
        ha="center",
        fontsize=14,
        weight="bold",
        transform=ax.transAxes,
    )
    ax.text(
        0.5,
        0.955,
        "sportsbetting-eventdriven — run locally; JPG is not stored in git",
        ha="center",
        fontsize=9,
        color="#555",
        transform=ax.transAxes,
    )

    box(
        ax, 0.05, 0.88, 0.9, 0.08,
        "PHYSICAL / VIRTUAL HARDWARE",
        ["vCPU · RAM · disk/NVMe · NIC · L1/L2/L3 CPU caches"],
        "#e8daef", "#8e44ad",
    )
    box(
        ax, 0.05, 0.78, 0.9, 0.08,
        "HYPERVISOR + LINUX KERNEL",
        ["KVM/hypervisor · kernel · page cache · cgroups v2 · TCP stack"],
        "#d5d8dc", "#2c3e50",
    )
    box(
        ax, 0.05, 0.66, 0.9, 0.09,
        "KUBERNETES (this worker node)",
        ["kubelet · containerd · CNI · Pod odds-service · Pod betting-service"],
        "#d4e6f1", "#2980b9",
    )
    box(
        ax, 0.05, 0.48, 0.42, 0.16,
        "CONTAINER → JVM (odds :8080)",
        ["Heap G1GC · metaspace · Tomcat threads", "Spring Boot · JPA · Kafka client"],
        "#fdebd0", "#d35400",
    )
    box(
        ax, 0.53, 0.48, 0.42, 0.16,
        "CONTAINER → JVM (betting :8084)",
        ["Heap G1GC · metaspace · Tomcat + @Scheduled", "Spring Security · JPA · Kafka consumer"],
        "#fdebd0", "#d35400",
    )
    box(
        ax, 0.05, 0.28, 0.9, 0.17,
        "SHARED ON CLUSTER (same or other nodes)",
        ["PostgreSQL · Kafka · Debezium · Keycloak · Prometheus"],
        "#fadbd8", "#c0392b",
    )
    box(
        ax, 0.05, 0.08, 0.9, 0.17,
        "EVENT FLOW",
        ["HTTP → odds DB+outbox → CDC → Kafka odds.updated.v1 → betting consumer → betting DB"],
        "#fcf3cf", "#f39c12",
    )

    for x in (0.26, 0.74):
        ax.add_patch(
            FancyArrowPatch(
                (x, 0.48), (x, 0.45),
                arrowstyle="-|>", mutation_scale=14, color="#c0392b", transform=ax.transAxes,
            )
        )
    ax.add_patch(
        FancyArrowPatch(
            (0.35, 0.55), (0.65, 0.55),
            arrowstyle="-|>", mutation_scale=14, color="#e67e22", transform=ax.transAxes,
        )
    )

    fig.savefig(OUT, format="jpeg", dpi=160, bbox_inches="tight", facecolor=BG)
    plt.close(fig)
    print(f"Wrote {OUT.resolve()}")


if __name__ == "__main__":
    main()
