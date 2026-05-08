package com.baseball.simulation.usecase.teamrecord.executor;

import com.baseball.simulation.entity.TeamRecord;
import com.baseball.simulation.repository.TeamRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * TeamRecord 생성 및 업데이트 전용 Usecase 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class TeamRecordExecutor {

    private final TeamRecordRepository teamRecordRepository;

    /**
     * 새 TeamRecord를 생성해 저장합니다.
     * (teamId, seasonYear) 조합이 유니크 제약으로 보호되므로 중복 생성 시 DB 예외 발생.
     */
    public TeamRecord create(Long teamId, int seasonYear) {
        return teamRecordRepository.save(TeamRecord.create(teamId, seasonYear));
    }

    /** 변경된 TeamRecord를 저장합니다. */
    public TeamRecord save(TeamRecord record) {
        return teamRecordRepository.save(record);
    }
}
