package kopo.poly.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * 기상청 단기예보 API 보조 유틸
 *
 * <ul>
 *   <li>위경도 → 기상청 격자(nx, ny) 변환 — 기상청 공개 Lambert Conformal Conic 공식</li>
 *   <li>초단기실황 발표 기준 일시(base_date, base_time) 계산</li>
 * </ul>
 */
public class KmaGridUtil {

    private static final double RE    = 6371.00877; // 지구 반경 (km)
    private static final double GRID  = 5.0;        // 격자 간격 (km)
    private static final double SLAT1 = 30.0;       // 투영 위도1 (degree)
    private static final double SLAT2 = 60.0;       // 투영 위도2 (degree)
    private static final double OLON  = 126.0;      // 기준점 경도 (degree)
    private static final double OLAT  = 38.0;       // 기준점 위도 (degree)
    private static final double XO    = 43;         // 기준점 X 좌표 (격자)
    private static final double YO    = 136;        // 기준점 Y 좌표 (격자)

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    /** 초단기실황은 매 정시 자료가 약 40분 이후 제공됨 */
    private static final int NCST_AVAILABLE_MINUTE = 40;

    private KmaGridUtil() {}

    /**
     * 위경도 → 기상청 격자 좌표
     *
     * @return int[]{nx, ny}
     */
    public static int[] toGrid(double lat, double lon) {
        double DEGRAD = Math.PI / 180.0;

        double re    = RE / GRID;
        double slat1 = SLAT1 * DEGRAD;
        double slat2 = SLAT2 * DEGRAD;
        double olon  = OLON * DEGRAD;
        double olat  = OLAT * DEGRAD;

        double sn = Math.tan(Math.PI * 0.25 + slat2 * 0.5) / Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sn = Math.log(Math.cos(slat1) / Math.cos(slat2)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + slat1 * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(slat1) / sn;
        double ro = Math.tan(Math.PI * 0.25 + olat * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        double ra = Math.tan(Math.PI * 0.25 + lat * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lon * DEGRAD - olon;
        if (theta > Math.PI)  theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        int nx = (int) Math.floor(ra * Math.sin(theta) + XO + 0.5);
        int ny = (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5);
        return new int[]{nx, ny};
    }

    /**
     * 초단기실황 조회 가능한 가장 최근 발표 일시 (KST)
     * — 현재 분이 40분 미만이면 한 시간 전 정시
     *
     * @return String[]{base_date(yyyyMMdd), base_time(HH00)}
     */
    public static String[] ultraSrtNcstBase() {
        LocalDateTime now = LocalDateTime.now(KST);
        if (now.getMinute() < NCST_AVAILABLE_MINUTE) now = now.minusHours(1);

        return new String[]{
                now.format(DateTimeFormatter.ofPattern("yyyyMMdd")),
                now.format(DateTimeFormatter.ofPattern("HH")) + "00"
        };
    }
}
