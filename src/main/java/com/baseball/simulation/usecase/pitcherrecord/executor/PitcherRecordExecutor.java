package com.baseball.simulation.usecase.pitcherrecord.executor;

import com.baseball.simulation.entity.PitcherRecord;
import com.baseball.simulation.repository.PitcherRecordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * PitcherRecord 생성 및 저장 전용 Usecase 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class PitcherRecordExecutor {

    private final PitcherRecordRepository pitcherRecordRepository;

    public PitcherRecord create(Long pitcherId, int seasonYear) {
        return pitcherRecordRepository.save(PitcherRecord.create(pitcherId, seasonYear));
    }

    /** 경기 종료 후 변경된 PitcherRecord 목록을 일괄 저장합니다. */
    public void saveAll(List<PitcherRecord> records) {
        pitcherRecordRepository.saveAll(records);
    }
}
