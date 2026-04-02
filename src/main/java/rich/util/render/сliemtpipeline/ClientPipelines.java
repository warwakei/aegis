package rich.util.render.сliemtpipeline;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderSetup;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.util.function.Function;

import static net.minecraft.client.gl.RenderPipelines.TRANSFORMS_AND_PROJECTION_SNIPPET;

public class ClientPipelines {

    public static final RenderPipeline ROMB_ESP_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/wtex")
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    public static final Function<Identifier, RenderLayer> ROMB_ESP =
            Util.memoize(texture -> {
                RenderSetup setup = RenderSetup.builder(ROMB_ESP_PIPELINE)
                        .texture("Sampler0", texture)
                        .translucent()
                        .expectedBufferSize(1536)
                        .build();
                return RenderLayer.of("wtex", setup);
            });

    public static final RenderPipeline GHOSTS_ESP_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/wtex")
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.LESS_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    public static final Function<Identifier, RenderLayer> GHOSTS_ESP =
            Util.memoize(texture -> {
                RenderSetup setup = RenderSetup.builder(GHOSTS_ESP_PIPELINE)
                        .texture("Sampler0", texture)
                        .translucent()
                        .expectedBufferSize(1536)
                        .build();
                return RenderLayer.of("wtex", setup);
            });

    public static final RenderPipeline CHAIN_ESP_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/wtex")
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    public static final Function<Identifier, RenderLayer> CHAIN_ESP =
            Util.memoize(texture -> {
                RenderSetup setup = RenderSetup.builder(CHAIN_ESP_PIPELINE)
                        .texture("Sampler0", texture)
                        .translucent()
                        .expectedBufferSize(1536)
                        .build();
                return RenderLayer.of("wtex", setup);
            });

    public static final RenderPipeline CRYSTAL_FILLED_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/crystal_filled")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    public static final RenderLayer CRYSTAL_FILLED = RenderLayer.of(
            "crystal_filled",
            RenderSetup.builder(CRYSTAL_FILLED_PIPELINE)
                    .translucent()
                    .expectedBufferSize(8192)
                    .build()
    );

    public static final RenderPipeline CRYSTAL_GLOW_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/crystal_glow")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    public static final RenderLayer CRYSTAL_GLOW = RenderLayer.of(
            "crystal_glow",
            RenderSetup.builder(CRYSTAL_GLOW_PIPELINE)
                    .translucent()
                    .expectedBufferSize(4096)
                    .build()
    );

    public static final RenderPipeline BLOOM_ESP_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/bloom_esp")
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    public static final Function<Identifier, RenderLayer> BLOOM_ESP =
            Util.memoize(texture -> {
                RenderSetup setup = RenderSetup.builder(BLOOM_ESP_PIPELINE)
                        .texture("Sampler0", texture)
                        .translucent()
                        .expectedBufferSize(2048)
                        .build();
                return RenderLayer.of("bloom_esp", setup);
            });

    public static final RenderPipeline CHINA_HAT_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/china_hat")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(true)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.TRIANGLE_FAN)
                    .build()
    );

    public static final RenderLayer CHINA_HAT = RenderLayer.of(
            "china_hat",
            RenderSetup.builder(CHINA_HAT_PIPELINE)
                    .translucent()
                    .expectedBufferSize(8192)
                    .build()
    );

    public static final RenderPipeline CHINA_HAT_OUTLINE_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/china_hat_outline")
                    .withVertexShader("core/position_color")
                    .withFragmentShader("core/position_color")
                    .withBlend(BlendFunction.TRANSLUCENT)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(true)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINE_STRIP)
                    .build()
    );

    public static final RenderLayer CHINA_HAT_OUTLINE = RenderLayer.of(
            "china_hat_outline",
            RenderSetup.builder(CHINA_HAT_OUTLINE_PIPELINE)
                    .translucent()
                    .expectedBufferSize(4096)
                    .build()
    );

    public static final RenderPipeline WORLD_PARTICLES_COLOR_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("rich", "world_particles_color"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    public static final RenderLayer WORLD_PARTICLES_QUADS = RenderLayer.of(
            "world_particles_cube",
            RenderSetup.builder(WORLD_PARTICLES_COLOR_PIPELINE)
                    .translucent()
                    .expectedBufferSize(2048)
                    .build()
    );

    public static final RenderPipeline WORLD_PARTICLES_LINES_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_COLOR_SNIPPET)
                    .withLocation(Identifier.of("rich", "world_particles_lines"))
                    .withVertexFormat(VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.DEBUG_LINES)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .build()
    );

    public static final RenderLayer WORLD_PARTICLES_LINES = RenderLayer.of(
            "world_particles_lines",
            RenderSetup.builder(WORLD_PARTICLES_LINES_PIPELINE)
                    .translucent()
                    .expectedBufferSize(2048)
                    .build()
    );

    public static final RenderPipeline WORLD_PARTICLES_GLOW_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(RenderPipelines.POSITION_TEX_COLOR_SNIPPET)
                    .withLocation(Identifier.of("rich", "world_particles_glow"))
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .withCull(false)
                    .withDepthTestFunction(DepthTestFunction.LEQUAL_DEPTH_TEST)
                    .withDepthWrite(false)
                    .withBlend(BlendFunction.LIGHTNING)
                    .withSampler("Sampler0")
                    .build()
    );

    public static final Function<Identifier, RenderLayer> WORLD_PARTICLES_GLOW =
            Util.memoize(texture -> {
                RenderSetup setup = RenderSetup.builder(WORLD_PARTICLES_GLOW_PIPELINE)
                        .texture("Sampler0", texture)
                        .translucent()
                        .expectedBufferSize(2048)
                        .build();
                return RenderLayer.of("world_particles_glow", setup);
            });

    public static final RenderPipeline GUI_ARROW_BLEND_PIPELINE = RenderPipelines.register(
            RenderPipeline.builder(TRANSFORMS_AND_PROJECTION_SNIPPET)
                    .withLocation("pipeline/gui_arrow_blend")
                    .withVertexShader("core/position_tex_color")
                    .withFragmentShader("core/position_tex_color")
                    .withSampler("Sampler0")
                    .withBlend(BlendFunction.LIGHTNING)
                    .withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
                    .withCull(false)
                    .withVertexFormat(VertexFormats.POSITION_TEXTURE_COLOR, VertexFormat.DrawMode.QUADS)
                    .build()
    );

    public static final Function<Identifier, RenderLayer> GUI_ARROW_BLEND =
            Util.memoize(texture -> {
                RenderSetup setup = RenderSetup.builder(GUI_ARROW_BLEND_PIPELINE)
                        .texture("Sampler0", texture)
                        .translucent()
                        .expectedBufferSize(256)
                        .build();
                return RenderLayer.of("gui_arrow_blend", setup);
            });
}