package io.github.yuupering.freemodelrotation;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Free Model Rotation — 클라이언트 진입점.
 *
 * <p>실제 기능은 전부 Mixin이 담당한다(진입점은 로드 확인 로그 용도):
 * <ul>
 *   <li>{@code ModelAngleLimitMixin} — 단일축 회전 각도 제한(22.5 배수 / ±45) 해제</li>
 *   <li>{@code ModelMultiAxisRotationMixin} — 다축({@code x/y/z}) 회전 역직렬화</li>
 *   <li>{@code BakedQuadFactoryMixin} — 다축 회전 베이킹(정점 변환)</li>
 * </ul>
 */
public class FreeModelRotationClient implements ClientModInitializer {

    public static final Logger LOGGER = LoggerFactory.getLogger("freemodelrotation");

    @Override
    public void onInitializeClient() {
        LOGGER.info("[FreeModelRotation] 모델 회전 각도 제한 해제 + 다축(x/y/z) 회전 활성화");
    }
}
