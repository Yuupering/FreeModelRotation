package io.github.yuupering.freemodelrotation.mixin;

import io.github.yuupering.freemodelrotation.util.MultiAxisRotations;
import net.minecraft.client.render.model.BakedQuadFactory;
import net.minecraft.client.render.model.json.ModelRotation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 다축(multi-axis) element 회전 베이킹 적용.
 *
 * <p>{@code ModelMultiAxisRotationMixin}이 {@link MultiAxisRotations#TABLE}에 등록한
 * placeholder {@link ModelRotation}이 들어오면, 바닐라 단일축 회전 대신
 * 3축 합성 회전을 정점에 적용한다.
 *
 * <p>회전 순서: 정점 기준 X→Y→Z (Quaternionf{@code .rotationZYX(z, y, x)} = Rz·Ry·Rx,
 * 정점에 X축 회전이 먼저 적용). 바닐라 25w46a 및 Blockbench와 동일한 순서다.
 * origin 기준 회전: {@code vertex = origin + R·(vertex - origin)}.
 *
 * <p>rescale 미지원: 다축 회전에서는 {@code "rescale": true}를 무시한다
 * (바닐라 rescale은 단일축 ±22.5/±45 전용 보정이라 임의 3축 합성에 의미가 없음).
 */
@Mixin(BakedQuadFactory.class)
public class BakedQuadFactoryMixin {

    @Inject(method = "rotateVertex", at = @At("HEAD"), cancellable = true)
    private void fmr$rotateMultiAxis(Vector3f vertex, ModelRotation rotation, CallbackInfo ci) {
        if (rotation == null) {
            return; // 바닐라도 즉시 return
        }
        float[] angles = MultiAxisRotations.TABLE.get(rotation);
        if (angles == null) {
            return; // 단일축(바닐라) 회전 — 가로채지 않음
        }
        Quaternionf q = new Quaternionf().rotationZYX(
                (float) Math.toRadians(angles[2]),
                (float) Math.toRadians(angles[1]),
                (float) Math.toRadians(angles[0]));
        Vector3f origin = rotation.origin();
        vertex.sub(origin);
        q.transform(vertex);
        vertex.add(origin);
        ci.cancel();
    }
}
