package fun.aegis.utils.display.font.entry;

import fun.aegis.utils.display.font.glyph.Glyph;

public record DrawEntry(float atX, float atY, int color, Glyph toDraw) {
}
