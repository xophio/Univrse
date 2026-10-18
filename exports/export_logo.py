from pathlib import Path

from PIL import Image, ImageDraw


def export() -> None:
    scale = 10
    output = Path(__file__).with_name("universe-logo-1080.png")
    image = Image.new("RGB", (108 * scale, 108 * scale), "#080908")
    draw = ImageDraw.Draw(image)

    def rect(left: int, top: int, right: int, bottom: int, color: str) -> None:
        draw.rectangle((left * scale, top * scale,
                        right * scale - 1, bottom * scale - 1), fill=color)

    # The Android vector contains only square-edged, integer-aligned paths.
    for bounds in [(42, 27, 66, 33), (30, 33, 78, 45), (24, 45, 84, 63),
                   (30, 63, 78, 75), (42, 75, 66, 81)]:
        rect(*bounds, "#DDF77F")
    for bounds in [(30, 39, 54, 45), (48, 45, 78, 51),
                   (24, 57, 42, 63), (36, 63, 78, 69)]:
        rect(*bounds, "#080908")
    image.save(output)


if __name__ == "__main__":
    export()
