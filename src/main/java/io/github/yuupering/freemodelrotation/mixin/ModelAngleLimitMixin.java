package io.github.yuupering.freemodelrotation.mixin;

import com.google.gson.JsonObject;
import net.minecraft.util.JsonHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 아이템/블록 모델 JSON의 element 회전 각도 제한 해제.
 *
 * <p>바닐라({@code ModelElement.Deserializer.deserializeRotationAngle})는 회전 각도를
 * -45 / -22.5 / 0 / 22.5 / 45 만 허용하고, 벗어나면 {@code JsonParseException}을 던져
 * 모델 로드를 막는다. 이 검증을 우회해 임의 각도(소수점, ±45 초과)를 그대로 허용한다.
 *
 * <p>다축 동시 회전은 {@code ModelMultiAxisRotationMixin}이 별도로 처리한다.
 *
 * <p>주의: 이 모드를 설치한 클라에서만 자유 각도 모델이 정상 렌더된다
 * (미설치 클라에선 해당 모델이 깨져 보일 수 있음). 또한 ±45를 크게 넘는 각도는
 * 바닐라 베이커 rescale 한계로 면이 잘리거나 라이팅이 어긋날 수 있다.
 */
@Mixin(targets = "net.minecraft.client.render.model.json.ModelElement$Deserializer")
public class ModelAngleLimitMixin {

    @Inject(method = "deserializeRotationAngle", at = @At("HEAD"), cancellable = true)
    private void fmr$allowAnyAngle(JsonObject object, CallbackInfoReturnable<Float> cir) {
        // 검증 없이 angle 값을 그대로 반환 → 22.5 배수/±45 제한 무시.
        cir.setReturnValue(JsonHelper.getFloat(object, "angle"));
    }
}
