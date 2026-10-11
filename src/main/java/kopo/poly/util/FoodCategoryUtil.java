package kopo.poly.util;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 식재료명 → 냉장고 분류 (키워드 매칭)
 *
 * <p>위에서부터 순서대로 검사 — 먼저 걸린 분류가 우선
 * (예: "닭가슴살햄" → 가공보다 육류가 먼저, "우유식빵" → 유제품).
 * 한 글자 키워드(무·파·쌀 등)는 이름 전체가 같을 때만 매칭해 오분류를 막습니다.</p>
 */
public final class FoodCategoryUtil {

    public static final String SEASONING = "SEASONING";
    public static final String ETC       = "ETC";

    private static final Map<String, List<String>> CATEGORY_KEYWORDS = new LinkedHashMap<>();

    static {
        CATEGORY_KEYWORDS.put(SEASONING, List.of(
                "간장", "고추장", "된장", "쌈장", "소금", "설탕", "식초", "참기름", "들기름", "마요네즈", "케첩", "케찹",
                "고춧가루", "후추", "다진마늘", "올리고당", "물엿", "꿀", "액젓", "굴소스", "식용유", "올리브유",
                "버터", "소스", "드레싱", "머스타드", "와사비", "맛술", "미림", "조청", "카레", "참깨", "깨"));
        CATEGORY_KEYWORDS.put("DAIRY_EGG", List.of(
                "우유", "치즈", "요거트", "요구르트", "요플레", "비요뜨", "생크림", "크림", "계란", "달걀", "메추리알", "두유"));
        CATEGORY_KEYWORDS.put("MEAT", List.of(
                "고기", "소고기", "쇠고기", "돼지", "삼겹", "목살", "등심", "안심", "갈비", "닭", "닭고기", "닭가슴", "닭다리", "닭날개", "닭봉", "오리", "양고기", "차돌"));
        CATEGORY_KEYWORDS.put("SEAFOOD", List.of(
                "생선", "고등어", "갈치", "연어", "참치", "새우", "오징어", "낙지", "문어", "쭈꾸미", "주꾸미", "조개",
                "바지락", "홍합", "굴", "전복", "멸치", "명태", "동태", "대구", "꽁치", "게", "꽃게", "미역", "다시마",
                "김", "톳", "해산물", "어패"));
        CATEGORY_KEYWORDS.put("PROCESSED", List.of(
                "햄", "소시지", "소세지", "베이컨", "스팸", "두부", "만두", "어묵", "김치", "단무지", "피클", "라면",
                "국수", "면", "볶음면", "짜장면", "냉면", "우동", "소면", "파스타", "스파게티", "마카로니", "떡",
                "밀가루", "부침가루", "튀김가루", "전분", "물", "생수", "탄산수", "견과", "빵", "식빵", "통조림", "캔", "냉동", "너겟", "돈까스", "돈가스", "맛살", "게맛살",
                "유부", "묵", "장아찌", "젓갈", "주스", "음료", "콜라", "사이다", "맥주"));
        CATEGORY_KEYWORDS.put("FRUIT", List.of(
                "사과", "배", "귤", "감귤", "오렌지", "레몬", "라임", "바나나", "포도", "샤인머스캣", "딸기", "블루베리",
                "수박", "참외", "멜론", "복숭아", "자두", "체리", "키위", "망고", "파인애플", "감", "홍시", "석류",
                "무화과", "아보카도", "자몽", "과일"));
        CATEGORY_KEYWORDS.put("VEGETABLE", List.of(
                "양파", "대파", "쪽파", "파", "마늘", "생강", "감자", "고구마", "당근", "무", "배추", "양배추", "상추",
                "깻잎", "시금치", "부추", "미나리", "쑥갓", "브로콜리", "콜리플라워", "오이", "호박", "애호박", "가지",
                "고추", "피망", "파프리카", "토마토", "방울토마토", "콩나물", "숙주", "버섯", "표고", "새송이",
                "느타리", "팽이", "양송이", "옥수수", "연근", "우엉", "도라지", "더덕", "청경채", "샐러드", "케일",
                "셀러리", "샐러리", "아스파라거스", "비트", "채소", "야채", "나물", "쌀", "콩", "현미", "잡곡"));
    }

    private FoodCategoryUtil() {
    }

    /**
     * @param name 식재료명
     * @param note 메모 — "양념" 포함 시 (분석 화면에서 추가한 양념·부재료) 무조건 양념
     * @return 분류 코드
     */
    public static String classify(String name, String note) {

        if (note != null && note.contains("양념")) {
            return SEASONING;
        }

        String nm = CmmUtil.nvl(name).replace(" ", "");
        if (nm.isEmpty()) {
            return ETC;
        }

        for (Map.Entry<String, List<String>> e : CATEGORY_KEYWORDS.entrySet()) {
            for (String kw : e.getValue()) {
                boolean hit = kw.length() == 1 ? nm.equals(kw) : nm.contains(kw);
                if (hit) {
                    return e.getKey();
                }
            }
        }

        return ETC;
    }
}
