package com.baseball.simulation.domain;

import com.baseball.simulation.entity.PitcherRecord;

/**
 * 경기 진행 중 인메모리에서 투수 성적을 추적하는 뮤터블 도메인 객체입니다.
 * <p>
 * [이닝 카운트 방식 - inningsX3]
 * 이닝을 아웃 수의 합산으로 관리합니다.
 * - STRIKEOUT / FIELD_OUT → inningsX3 += 1 (아웃 1개 기록)
 * - 출력 시: 20 → "6.2이닝",  27 → "9.0이닝"
 * <p>
 * [wins / losses / games 업데이트 시점]
 * 경기 시뮬레이션 종료 후 GameFacade에서 승패를 판정해 addWin() / addLoss() / addGame()을 호출합니다.
 * 이 세 값은 시뮬레이션 중 InningProcessor에서 업데이트하지 않습니다.
 * <p>
 * TODO(part2): earnedRuns(자책점) 구분 추가 → ERA 정확도 향상
 */
public class PitcherStatSnapshot {

    private final Long id;
    private final Long pitcherId;
    private final int  seasonYear;

    private int games;
    private int wins;
    private int losses;
    private int strikeouts;
    private int walks;
    private int inningsX3;
    private int hitsAllowed;
    private int homeRunsAllowed;
    private int battersFaced;
    private int pitchesThrown;
    private int runsAllowed;

    public static PitcherStatSnapshot from(PitcherRecord record) {
        PitcherStatSnapshot snap = new PitcherStatSnapshot(
                record.getId(), record.getPitcherId(), record.getSeasonYear());
        snap.games           = record.getGames();
        snap.wins            = record.getWins();
        snap.losses          = record.getLosses();
        snap.strikeouts      = record.getStrikeouts();
        snap.walks           = record.getWalks();
        snap.inningsX3       = record.getInningsX3();
        snap.hitsAllowed     = record.getHitsAllowed();
        snap.homeRunsAllowed = record.getHomeRunsAllowed();
        snap.battersFaced    = record.getBattersFaced();
        snap.pitchesThrown   = record.getPitchesThrown();
        snap.runsAllowed     = record.getRunsAllowed();
        return snap;
    }

    public static PitcherStatSnapshot blank(Long pitcherId, int seasonYear) {
        return new PitcherStatSnapshot(null, pitcherId, seasonYear);
    }

    private PitcherStatSnapshot(Long id, Long pitcherId, int seasonYear) {
        this.id         = id;
        this.pitcherId  = pitcherId;
        this.seasonYear = seasonYear;
    }

    // -----------------------------------------------------------------------
    // 타석 결과 반영 (InningProcessor에서 호출)
    // -----------------------------------------------------------------------

    /**
     * 한 타석의 투구·결과를 인메모리 수치에 즉시 반영합니다.
     *
     * @param paResultCode  타석 결과 코드
     * @param pitchCount    이 타석에서 던진 투구 수
     * @param runsInThisPA  이 타석에서 허용한 실점
     */
    public void applyPaResult(String paResultCode, int pitchCount, int runsInThisPA) {
        battersFaced++;
        pitchesThrown += pitchCount;
        runsAllowed   += runsInThisPA;

        switch (paResultCode) {
            case "STRIKEOUT" -> { strikeouts++; inningsX3++; }
            case "FIELD_OUT" -> inningsX3++;
            case "WALK"      -> walks++;
            case "SINGLE", "DOUBLE", "TRIPLE" -> hitsAllowed++;
            case "HOMERUN"   -> { hitsAllowed++; homeRunsAllowed++; }
            default          -> { /* 미지원 결과 */ }
        }
    }

    // -----------------------------------------------------------------------
    // 경기 종료 후 승패 반영 (GameFacade에서 호출)
    // -----------------------------------------------------------------------

    public void addGame() { games++; }
    public void addWin()  { wins++; }
    public void addLoss() { losses++; }

    // -----------------------------------------------------------------------
    // 파생 지표 계산 (분모 0 예외 처리 포함)
    // -----------------------------------------------------------------------

    /**
     * 평균자책점(ERA) = (runsAllowed * 9) / (inningsX3 / 3.0)
     * 현재는 자책점 구분 없이 실점으로 계산합니다.
     */
    public double era() {
        return inningsX3 == 0 ? 0.0 : (runsAllowed * 9.0) / (inningsX3 / 3.0);
    }

    /**
     * 피안타율 = hitsAllowed / (battersFaced - walks)
     * 공식 타수에서 볼넷을 제외한 값으로 계산합니다.
     */
    public double avgAllowed() {
        int officialAB = battersFaced - walks;
        return officialAB == 0 ? 0.0 : (double) hitsAllowed / officialAB;
    }

    /**
     * WHIP = (hitsAllowed + walks) / (inningsX3 / 3.0)
     */
    public double whip() {
        return inningsX3 == 0 ? 0.0 : (hitsAllowed + walks) / (inningsX3 / 3.0);
    }

    /** 출력용 이닝 문자열. 예) inningsX3=20 → "6.2" */
    public String inningsDisplay() {
        return String.format("%d.%d", inningsX3 / 3, inningsX3 % 3);
    }

    // -----------------------------------------------------------------------
    // Getters
    // -----------------------------------------------------------------------

    public Long getId()              { return id; }
    public Long getPitcherId()       { return pitcherId; }
    public int  getSeasonYear()      { return seasonYear; }
    public int  getGames()           { return games; }
    public int  getWins()            { return wins; }
    public int  getLosses()          { return losses; }
    public int  getStrikeouts()      { return strikeouts; }
    public int  getWalks()           { return walks; }
    public int  getInningsX3()       { return inningsX3; }
    public int  getHitsAllowed()     { return hitsAllowed; }
    public int  getHomeRunsAllowed() { return homeRunsAllowed; }
    public int  getBattersFaced()    { return battersFaced; }
    public int  getPitchesThrown()   { return pitchesThrown; }
    public int  getRunsAllowed()     { return runsAllowed; }
}
