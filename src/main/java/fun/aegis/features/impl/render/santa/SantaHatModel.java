package fun.aegis.features.impl.render.santa;

import net.minecraft.client.model.*;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

public class SantaHatModel {
   private final ModelPart root;
   private final ModelPart santaHat;

   public SantaHatModel() {
      this.root = createModel();
      this.santaHat = this.root.getChild("santa_hat");
   }

   private static ModelPart createModel() {
      ModelData modelData = new ModelData();
      ModelPartData modelPartData = modelData.getRoot();
      
      ModelPartData santaHat = modelPartData.addChild("santa_hat", 
          ModelPartBuilder.create(),
          ModelTransform.of(0.0F, 0.0F, 0.0F, 0.0F, -90.0F * 0.017453292F, 0.0F)
      );
      
      santaHat.addChild("sant_hat_top2", 
          ModelPartBuilder.create()
              .uv(0, 0)
              .cuboid(-0.5F, 2.0F, -1.5F, 3.0F, 3.0F, 3.0F), 
          ModelTransform.of(0.53024F, 10.61642F, 0.0F, 0.0F, 0.0F, -60.0F * 0.017453292F)
      );
      
      santaHat.addChild("sant_hat_top1", 
          ModelPartBuilder.create()
              .uv(0, 0)
              .cuboid(-3.0F, -1.0F, -3.0F, 6.0F, 4.0F, 6.0F), 
          ModelTransform.of(1.03024F, 9.75039F, 0.0F, 0.0F, 0.0F, -50.0F * 0.017453292F)
      );
      
      santaHat.addChild("santa_hat_top0", 
          ModelPartBuilder.create()
              .uv(0, 0)
              .cuboid(-4.0F, -1.0F, -4.0F, 9.0F, 3.0F, 8.0F), 
          ModelTransform.of(0.2892F, 8.72718F, 0.0F, 0.0F, 0.0F, -20.0F * 0.017453292F)
      );
      
      santaHat.addChild("sant_hat_top3", 
          ModelPartBuilder.create()
              .uv(0, 16)
              .cuboid(-0.90192F, -1.83013F, -2.0F, 4.0F, 4.0F, 4.0F), 
          ModelTransform.of(5.78024F, 12.11642F, 0.0F, 0.0F, 0.0F, 15.0F * 0.017453292F)
      );
      
      santaHat.addChild("santa_hat_base", 
          ModelPartBuilder.create()
              .uv(0, 16)
              .cuboid(-4.5F, -3.0F, -4.5F, 10.0F, 4.0F, 9.0F, new Dilation(0.1F)), 
          ModelTransform.of(0.2892F, 8.72718F, 0.0F, 0.0F, 0.0F, -15.0F * 0.017453292F)
      );
      
      return modelPartData.createPart(64, 32);
   }

   public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, Identifier texture) {
      VertexConsumer vertexConsumer = vertexConsumers.getBuffer(RenderLayer.getEntityCutoutNoCull(texture));
      this.root.render(matrices, vertexConsumer, light, OverlayTexture.DEFAULT_UV);
   }
}
