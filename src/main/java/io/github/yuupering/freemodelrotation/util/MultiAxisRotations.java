package io.github.yuupering.freemodelrotation.util;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.render.model.json.ModelRotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 다축(multi-axis) element 회전 사이드 테이블.
 *
 * <p>바닐라 {@link ModelRotation}은 단일 축(axis 1개 + angle 1개)만 표현할 수 있다.
 * Blockbench(snapshot 25w46a / 1.21.11+ 포맷)가 내보내는 다축 큐브의
 * {@code "rotation": {"x":xDeg, "y":yDeg, "z":zDeg, "origin":[...]}} 형식을 지원하기 위해,
 * 역직렬화 단계({@code ModelMultiAxisRotationMixin})에서 placeholder
 * {@link ModelRotation}을 만들어 키로 쓰고 실제 3축 각도(도 단위 {@code [x,y,z]})를
 * 이 테이블에 보관한다. 베이킹 단계({@code BakedQuadFactoryMixin})에서 키가 조회되면
 * 바닐라 단일축 회전 대신 X→Y→Z 순서의 합성 회전을 적용한다.
 *
 * <p>스레드 안전: 모델 로딩/베이킹은 병렬 ForkJoin 워커에서 돌므로
 * {@link Collections#synchronizedMap}으로 감싼다. 약한 키(WeakHashMap)라서
 * 리소스 리로드로 언베이크드 모델이 버려지면 엔트리도 함께 수거된다
 * (값 {@code float[]}는 키를 참조하지 않으므로 누수 없음).
 */
public final class MultiAxisRotations {

    public static final Logger LOGGER = LoggerFactory.getLogger("freemodelrotation");

    /** placeholder {@link ModelRotation} → 실제 다축 각도 {@code [xDeg, yDeg, zDeg]}. */
    public static final Map<ModelRotation, float[]> TABLE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private static final AtomicInteger SEQ = new AtomicInteger();

    private MultiAxisRotations() {
    }

    /**
     * placeholder용 sentinel 각도 발급.
     *
     * <p>{@link ModelRotation}은 record(값 동등성)라서 origin이 같은 두 다축 회전이
     * 같은 placeholder 값을 쓰면 WeakHashMap 키가 충돌해 한쪽 각도를 덮어쓴다.
     * 엔트리마다 유일한 sentinel 각도를 부여해 키를 구분한다.
     * -1,000,001부터 1씩 감소 — 2^24 미만 정수라 float로 정확히 표현되고,
     * 실제 모델이 쓸 리 없는 값이라 바닐라 단일축 회전과 충돌하지 않는다.
     * (이 각도는 베이킹 시 믹스인이 회전을 가로채므로 렌더에 쓰이지 않는다.)
     */
    public static float nextPlaceholderAngle() {
        return -1_000_001.0F - SEQ.getAndIncrement();
    }
}
