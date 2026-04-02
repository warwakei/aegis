package fun.aegis.utils.display.systemrender.builders.states;

import java.awt.*;

public record QuadColorState(int color1, int color2, int color3, int color4) {

	public static final QuadColorState TRANSPARENT = new QuadColorState(0, 0, 0, 0);
	public static final QuadColorState WHITE = new QuadColorState(-1, -1, -1, -1);

	public QuadColorState(Color color1, Color color2, Color color3, Color color4) {
		this(color1.getRGB(), color2.getRGB(), color3.getRGB(), color4.getRGB());
	}

	public QuadColorState(Color color) {
		this(color, color, color, color);
	}

	public QuadColorState(int color) {
		this(color, color, color, color);
	}

	/**
	 * Создает цвет из RGBA компонентов.
	 * Этот метод напрямую вычисляет итоговый цвет, не используя классы AWT.
	 * @param r Красный компонент (0-255)
	 * @param g Зеленый компонент (0-255)
	 * @param b Синий компонент (0-255)
	 * @param a Альфа-компонент (прозрачность, 0-255)
	 * @return Новый экземпляр QuadColorState с заданным цветом.
	 */
	public static QuadColorState fromRgba(int r, int g, int b, int a) {
		// Пакуем цвет в формат ARGB (Alpha, Red, Green, Blue), который используется в Minecraft
		int packedColor = (a & 0xFF) << 24 | (r & 0xFF) << 16 | (g & 0xFF) << 8 | (b & 0xFF);
		return new QuadColorState(packedColor);
	}

	/**
	 * Линейно интерполирует между двумя цветами.
	 * @param c1 Первый цвет.
	 * @param c2 Второй цвет.
	 * @param ratio Коэффициент смешивания (0.0 для c1, 1.0 для c2).
	 * @return Интерполированный цвет.
	 */
	private static Color interpolate(Color c1, Color c2, float ratio) {
		ratio = Math.max(0, Math.min(1, ratio));
		int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
		int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
		int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
		int a = (int) (c1.getAlpha() * (1 - ratio) + c2.getAlpha() * ratio);
		return new Color(r, g, b, a);
	}

	/**
	 * Создает состояние цвета с вертикальным градиентом.
	 * @param topColor Цвет для верхних вершин.
	 * @param bottomColor Цвет для нижних вершин.
	 * @return Новый QuadColorState, представляющий вертикальный градиент.
	 */
	public static QuadColorState vertical(Color topColor, Color bottomColor) {
		// Предполагаемый порядок вершин: верх-лево, низ-лево, низ-право, верх-право
		return new QuadColorState(topColor, bottomColor, bottomColor, topColor);
	}

	public static QuadColorState vertical(int topColor, int bottomColor) {
		return new QuadColorState(topColor, bottomColor, bottomColor, topColor);
	}

	/**
	 * Создает состояние цвета с горизонтальным градиентом.
	 * @param leftColor Цвет для левых вершин.
	 * @param rightColor Цвет для правых вершин.
	 * @return Новый QuadColorState, представляющий горизонтальный градиент.
	 */
	public static QuadColorState horizontal(Color leftColor, Color rightColor) {
		// Предполагаемый порядок вершин: верх-лево, низ-лево, низ-право, верх-право
		return new QuadColorState(leftColor, leftColor, rightColor, rightColor);
	}

	public static QuadColorState horizontal(int leftColor, int rightColor) {
		return new QuadColorState(leftColor, leftColor, rightColor, rightColor);
	}

	/**
	 * Создает анимированный вертикальный градиент, который циклически меняется между двумя цветами.
	 * Для анимации необходимо вызывать этот метод в каждом кадре.
	 * @param color1 Первый цвет.
	 * @param color2 Второй цвет.
	 * @param durationSeconds Длительность полного цикла анимации в секундах.
	 * @return Новый QuadColorState для текущего кадра анимации.
	 */
	public static QuadColorState animatedVertical(Color color1, Color color2, double durationSeconds) {
		// Используем синусоиду для плавной "пинг-понг" анимации
		double progress = (System.currentTimeMillis() % (durationSeconds * 1000.0)) / (durationSeconds * 1000.0);
		float blend = (float) (Math.sin(progress * 2.0 * Math.PI) * 0.5 + 0.5);

		Color topColor = interpolate(color1, color2, blend);
		Color bottomColor = interpolate(color2, color1, blend);

		return vertical(topColor, bottomColor);
	}

	/**
	 * Создает анимированный горизонтальный градиент, который циклически меняется между двумя цветами.
	 * Для анимации необходимо вызывать этот метод в каждом кадре.
	 * @param color1 Первый цвет.
	 * @param color2 Второй цвет.
	 * @param durationSeconds Длительность полного цикла анимации в секундах.
	 * @return Новый QuadColorState для текущего кадра анимации.
	 */
	public static QuadColorState animatedHorizontal(Color color1, Color color2, double durationSeconds) {
		double progress = (System.currentTimeMillis() % (durationSeconds * 1000.0)) / (durationSeconds * 1000.0);
		float blend = (float) (Math.sin(progress * 2.0 * Math.PI) * 0.5 + 0.5);

		Color leftColor = interpolate(color1, color2, blend);
		Color rightColor = interpolate(color2, color1, blend);

		return horizontal(leftColor, rightColor);
	}
}
