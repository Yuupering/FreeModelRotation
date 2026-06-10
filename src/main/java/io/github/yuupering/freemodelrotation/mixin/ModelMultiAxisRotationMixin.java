package io.github.yuupering.freemodelrotation.mixin;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import io.github.yuupering.freemodelrotation.util.MultiAxisRotations;
import net.minecraft.client.render.model.json.ModelRotation;
import net.minecraft.util.JsonHelper;
import net.minecraft.util.math.Direction;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 아이템/블록 모델 JSON element의 다축(multi-axis) 회전 역직렬화 지원.
 *
 * <p>허용하는 rotation 형식:
 * <ul>
 *   <li>바닐라 단일축 — {@code {"origin":[...], "axis":"x|y|z", "angle": N}}:
 *       가로채지 않고 바닐라 경로 그대로(각도 제한은 {@link ModelAngleLimitMixin}이 이미 해제).</li>
 *   <li>다축 (Blockbench 25w46a/1.21.11+ 포맷) —
 *       {@code {"origin":[...], "x":xDeg, "y":yDeg, "z":zDeg}} (axis/angle 없음):
 *       origin은 바닐라와 동일하게 파싱(0.0625 스케일)하고, placeholder
 *       {@link ModelRotation}을 만들어 {@link MultiAxisRotations#TABLE}에 실제 3축 각도를
 *       등록한다. 적용 순서는 X→Y→Z({@code BakedQuadFactoryMixin} 참고), rescale 미지원.</li>
 *   <li>다축 (배열 표기) — {@code {"origin":[...], "angle":[xDeg,yDeg,zDeg]}}:
 *       위와 동일하게 처리(일부 툴이 내보내는 대체 표기).</li>
 *   <li>방어 — axis 누락 + angle 스칼라(또는 누락/길이 불일치 배열): 모델 전체 로드 실패
 *       (보라-검정 누락 텍스처) 대신 경고 1줄 남기고 해당 회전만 무시(null 반환).</li>
 * </ul>
 *
 * <p>바닐라 {@code deserializeRotation}은 "rotation" 키가 없으면 null을 반환하므로
 * 그 경우는 건드리지 않는다. rotation 값이 객체가 아닌 형식 오류도 바닐라 에러에 위임한다.
 */
@Mixin(targets = "net.minecraft.client.render.model.json.ModelElement$Deserializer")
public class ModelMultiAxisRotationMixin {

    @Inject(method = "deserializeRotation", at = @At("HEAD"), cancellable = true)
    private void fmr$multiAxisRotation(JsonObject object, CallbackInfoReturnable<ModelRotation> cir) {
        if (!object.has("rotation")) {
            return; // 회전 없음 — 바닐라가 null 반환
        }
        JsonElement rotationEl = object.get("rotation");
        if (!rotationEl.isJsonObject()) {
            return; // 형식 오류 — 바닐라 에러 경로에 위임
        }
        JsonObject rotation = rotationEl.getAsJsonObject();
        JsonElement angleEl = rotation.get("angle");
        boolean angleIsArray = angleEl != null && angleEl.isJsonArray();
        // Blockbench 다축 export — {"x":deg,"y":deg,"z":deg,"origin":[...]} (axis/angle 없음).
        boolean hasXYZ = rotation.has("x") || rotation.has("y") || rotation.has("z");

        if (rotation.has("axis") && !angleIsArray && !hasXYZ) {
            return; // 바닐라 단일축 경로 그대로
        }

        if (hasXYZ) {
            Vector3f origin = fmr$parseOrigin(rotation);
            float x = JsonHelper.getFloat(rotation, "x", 0.0F);
            float y = JsonHelper.getFloat(rotation, "y", 0.0F);
            float z = JsonHelper.getFloat(rotation, "z", 0.0F);
            ModelRotation placeholder = new ModelRotation(
                    origin, Direction.Axis.Z, MultiAxisRotations.nextPlaceholderAngle(), false);
            MultiAxisRotations.TABLE.put(placeholder, new float[]{x, y, z});
            cir.setReturnValue(placeholder);
            return;
        }

        if (angleIsArray) {
            JsonArray angles = angleEl.getAsJsonArray();
            if (angles.size() == 3) {
                Vector3f origin = fmr$parseOrigin(rotation);
                float x = JsonHelper.asFloat(angles.get(0), "angle[0]");
                float y = JsonHelper.asFloat(angles.get(1), "angle[1]");
                float z = JsonHelper.asFloat(angles.get(2), "angle[2]");
                ModelRotation placeholder = new ModelRotation(
                        origin, Direction.Axis.Z, MultiAxisRotations.nextPlaceholderAngle(), false);
                MultiAxisRotations.TABLE.put(placeholder, new float[]{x, y, z});
                cir.setReturnValue(placeholder);
                return;
            }
            MultiAxisRotations.LOGGER.warn(
                    "[FreeModelRotation] 모델 rotation angle 배열 길이 {} (3 필요) — 해당 회전 무시", angles.size());
            cir.setReturnValue(null);
            return;
        }

        // axis 누락 + angle 스칼라(또는 누락) — 모델은 살리고 회전만 무시
        MultiAxisRotations.LOGGER.warn("[FreeModelRotation] 모델 rotation axis 누락 — 해당 회전 무시");
        cir.setReturnValue(null);
    }

    /**
     * 바닐라 {@code deserializeVec3f(rotation, "origin")} + {@code mul(0.0625F)} 복제.
     * (origin 좌표는 16분의 1 블록 공간으로 스케일되어 베이커의 정점 좌표와 일치해야 한다.)
     */
    @Unique
    private static Vector3f fmr$parseOrigin(JsonObject rotation) {
        JsonArray array = JsonHelper.getArray(rotation, "origin");
        if (array.size() != 3) {
            throw new JsonParseException("Expected 3 origin values, found: " + array.size());
        }
        float[] fs = new float[3];
        for (int i = 0; i < fs.length; i++) {
            fs[i] = JsonHelper.asFloat(array.get(i), "origin[" + i + "]");
        }
        return new Vector3f(fs[0], fs[1], fs[2]).mul(0.0625F);
    }
}
