package fun.aegis.features.impl.render.santa;

import fun.aegis.features.impl.render.SantaHatModule;
import fun.aegis.common.repository.friend.FriendUtils;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class SantaHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {
   private final SantaHatModel hatModel = new SantaHatModel();
   private static final Identifier TEXTURE = Identifier.of("minecraft", "textures/santa.png");

   public SantaHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
      super(context);
   }

   @Override
   public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, 
                      PlayerEntityRenderState entityState, float limbAngle, float limbDistance) {
      if (this.shouldRenderHat(entityState)) {
         matrices.push();
         PlayerEntityModel model = this.getContextModel();
         model.head.rotate(matrices);
         matrices.scale(1.0F, -1.0F, 1.0F);
         this.hatModel.render(matrices, vertexConsumers, light, TEXTURE);
         matrices.pop();
      }
   }

   private boolean shouldRenderHat(PlayerEntityRenderState entityState) {
      if (this.isModuleEnabled()) {
         return true;
      }
      
      // Friend check disabled due to API changes
      return false;
   }

   private boolean isModuleEnabled() {
      try {
         SantaHatModule module = SantaHatModule.getInstance();
         return module != null && module.isState();
      } catch (Exception e) {
         return false;
      }
   }
}
