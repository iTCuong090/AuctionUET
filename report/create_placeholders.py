from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


ROOT = Path(__file__).resolve().parent / "images"
ROOT.mkdir(parents=True, exist_ok=True)

ITEMS = [
    (
        "system_architecture.png",
        "System Architecture",
        "Client - Protocol - Server - JSON Persistence",
    ),
    (
        "domain_class_interfaces.png",
        "Domain Classes & Interfaces",
        "User roles, LiveAuction, Observer, AutoBid",
    ),
    (
        "realtime_bidding_flow.png",
        "Realtime Bidding Flow",
        "PLACE_BID request, Observer push, JavaFX chart update",
    ),
]


def centered_text(draw, width, y, text, font, fill):
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    draw.text(((width - text_width) / 2, y), text, fill=fill, font=font)


def main():
    font_title = ImageFont.truetype(r"C:\Windows\Fonts\timesbd.ttf", 72)
    font_sub = ImageFont.truetype(r"C:\Windows\Fonts\times.ttf", 36)
    font_note = ImageFont.truetype(r"C:\Windows\Fonts\times.ttf", 30)

    for filename, title, subtitle in ITEMS:
        img = Image.new("RGB", (1600, 900), "#f8fafc")
        draw = ImageDraw.Draw(img)

        draw.rectangle((28, 28, 1572, 872), outline="#94a3b8", width=4)
        draw.rectangle((80, 100, 1520, 800), outline="#cbd5e1", width=3)
        draw.line((120, 275, 1480, 275), fill="#e2e8f0", width=3)

        blocks = [
            (170, 380, 470, 560, "CLIENT"),
            (650, 380, 950, 560, "PROTOCOL"),
            (1130, 380, 1430, 560, "SERVER"),
        ]
        for x1, y1, x2, y2, text in blocks:
            draw.rounded_rectangle(
                (x1, y1, x2, y2),
                radius=18,
                fill="#ffffff",
                outline="#64748b",
                width=3,
            )
            bbox = draw.textbbox((0, 0), text, font=font_note)
            draw.text(
                ((x1 + x2 - (bbox[2] - bbox[0])) / 2, (y1 + y2 - (bbox[3] - bbox[1])) / 2),
                text,
                fill="#334155",
                font=font_note,
            )

        draw.line((470, 470, 650, 470), fill="#475569", width=5)
        draw.polygon([(650, 470), (625, 456), (625, 484)], fill="#475569")
        draw.line((950, 470, 1130, 470), fill="#475569", width=5)
        draw.polygon([(1130, 470), (1105, 456), (1105, 484)], fill="#475569")

        centered_text(draw, 1600, 145, title, font_title, "#0f172a")
        centered_text(draw, 1600, 235, subtitle, font_sub, "#334155")
        centered_text(
            draw,
            1600,
            690,
            "PLACEHOLDER - replace after Mermaid render",
            font_note,
            "#dc2626",
        )
        img.save(ROOT / filename, "PNG")


if __name__ == "__main__":
    main()
