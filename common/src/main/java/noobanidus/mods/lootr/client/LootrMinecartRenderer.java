package noobanidus.mods.lootr.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.MinecartRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.mojang.math.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import noobanidus.mods.lootr.entity.LootrChestMinecartEntity;
import noobanidus.mods.lootr.tiles.SpecialLootChestTile;

@OnlyIn(Dist.CLIENT)
public class LootrMinecartRenderer extends MinecartRenderer<LootrChestMinecartEntity> {
  private final SpecialLootChestTile tile = new SpecialLootChestTile();

  public LootrMinecartRenderer(EntityRenderDispatcher renderManagerIn) {
    super(renderManagerIn);
  }

  @Override
  public void render(LootrChestMinecartEntity entityIn, float entityYaw, float partialTicks, PoseStack matrixStackIn, MultiBufferSource bufferIn, int packedLightIn) {
    super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    matrixStackIn.pushPose();
    long i = (long) entityIn.getId() * 493286711L;
    i = i * i * 4392167121L + i * 98761L;
    float f = (((float) (i >> 16 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
    float f1 = (((float) (i >> 20 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
    float f2 = (((float) (i >> 24 & 7L) + 0.5F) / 8.0F - 0.5F) * 0.004F;
    matrixStackIn.translate(f, f1, f2);
    double d0 = Mth.lerp(partialTicks, entityIn.xOld, entityIn.getX());
    double d1 = Mth.lerp(partialTicks, entityIn.yOld, entityIn.getY());
    double d2 = Mth.lerp(partialTicks, entityIn.zOld, entityIn.getZ());
    Vec3 vector3d = entityIn.getPos(d0, d1, d2);
    float f3 = Mth.lerp(partialTicks, entityIn.xRotO, entityIn.xRot);
    if (vector3d != null) {
      Vec3 vector3d1 = entityIn.getPosOffs(d0, d1, d2, 0.3F);
      Vec3 vector3d2 = entityIn.getPosOffs(d0, d1, d2, -0.3F);
      if (vector3d1 == null) {
        vector3d1 = vector3d;
      }

      if (vector3d2 == null) {
        vector3d2 = vector3d;
      }

      matrixStackIn.translate(vector3d.x - d0, (vector3d1.y + vector3d2.y) / 2.0D - d1, vector3d.z - d2);
      Vec3 vector3d3 = vector3d2.add(-vector3d1.x, -vector3d1.y, -vector3d1.z);
      if (vector3d3.length() != 0.0D) {
        vector3d3 = vector3d3.normalize();
        entityYaw = (float) (Math.atan2(vector3d3.z, vector3d3.x) * 180.0D / Math.PI);
        f3 = (float) (Math.atan(vector3d3.y) * 73.0D);
      }
    }

    matrixStackIn.translate(0.0D, 0.375D, 0.0D);
    matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(180.0F - entityYaw));
    matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(-f3));
    float f5 = (float) entityIn.getHurtTime() - partialTicks;
    float f6 = entityIn.getDamage() - partialTicks;
    if (f6 < 0.0F) {
      f6 = 0.0F;
    }

    if (f5 > 0.0F) {
      matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(Mth.sin(f5) * f5 * f6 / 10.0F * (float) entityIn.getHurtDir()));
    }

    int j = entityIn.getDisplayOffset();
    matrixStackIn.pushPose();
    matrixStackIn.scale(0.75F, 0.75F, 0.75F);
    matrixStackIn.translate(-0.5D, (float) (j - 8) / 16.0F, 0.5D);
    matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(90.0F));
    if (entityIn.isOpened()) {
      tile.setOpened(true);
    } else {
      tile.setOpened(false);
    }
    BlockEntityRenderDispatcher.instance.renderItem(tile, matrixStackIn, bufferIn, packedLightIn, OverlayTexture.NO_OVERLAY);
    matrixStackIn.popPose();

    matrixStackIn.scale(-1.0F, -1.0F, 1.0F);
    this.model.setupAnim(entityIn, 0.0F, 0.0F, -0.1F, 0.0F, 0.0F);
    VertexConsumer ivertexbuilder = bufferIn.getBuffer(this.model.renderType(this.getTextureLocation(entityIn)));
    this.model.renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
    matrixStackIn.popPose();
  }
}
