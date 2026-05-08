package com.baseball.simulation.usecase.batterrecord.executor;

import com.baseball.simulation.entity.BatterRecord;
import com.baseball.simulation.repository.BatterRecordRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * BatterRecord 생성 및 저장 전용 Usecase 컴포넌트입니다.
 */
@Component
@RequiredArgsConstructor
public class BatterRecordExecutor {

    private final BatterRecordRepository batterRecordRepository;

    /** 새 BatterRecord를 생성해 저장합니다. */
    public BatterRecord create(Long batterId, int seasonYear) {
        return batterRecordRepository.save(BatterRecord.create(batterId, seasonYear));
    }

    /** 경기 종료 후 변경된 BatterRecord 목록을 일괄 저장합니다. */
    public void saveAll(List<BatterRecord> records) {
        batterRecordRepository.saveAll(records);
    }
}
