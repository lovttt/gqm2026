package com.gqm2026.school.application;

import com.gqm2026.school.domain.QuotaGroupService;
import com.gqm2026.school.entity.QuotaSeat;
import com.gqm2026.school.repository.QuotaSeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 校额名额应用服务。
 * 名额 CRUD + 分组查询（分组合并下沉为 QuotaGroupService 领域服务）。
 */
@Service
@RequiredArgsConstructor
public class QuotaSeatAppService {

    private final QuotaSeatRepository quotaSeatRepository;
    private final QuotaGroupService quotaGroupService;

    @Transactional(readOnly = true)
    public Page<QuotaSeat> listQuotaSeats(Long juniorSchoolId, Long highSchoolId, Pageable pageable) {
        if (juniorSchoolId != null && highSchoolId != null) {
            return quotaSeatRepository.findByJuniorSchoolIdAndHighSchoolId(juniorSchoolId, highSchoolId, pageable);
        }
        if (juniorSchoolId != null) {
            Page<QuotaSeat> merged = quotaGroupService.mergeForJunior(juniorSchoolId, pageable);
            if (merged != null) {
                return merged;
            }
            return quotaSeatRepository.findByJuniorSchoolId(juniorSchoolId, pageable);
        }
        if (highSchoolId != null) {
            return quotaGroupService.mergeForHighSchool(highSchoolId,
                    quotaSeatRepository.findByHighSchoolId(highSchoolId), pageable);
        }
        return quotaSeatRepository.findAll(pageable);
    }

    @Transactional
    public QuotaSeat createOrUpdateQuotaSeat(QuotaSeat quotaSeat) {
        QuotaSeat existing = quotaSeatRepository
                .findByJuniorSchoolIdAndHighSchoolId(quotaSeat.getJuniorSchoolId(), quotaSeat.getHighSchoolId());
        if (existing != null) {
            existing.setQuota(quotaSeat.getQuota());
            return quotaSeatRepository.save(existing);
        }
        quotaSeat.setId(null);
        return quotaSeatRepository.save(quotaSeat);
    }

    @Transactional
    public QuotaSeat updateQuotaSeat(Long id, QuotaSeat quotaSeat) {
        quotaSeat.setId(id);
        return quotaSeatRepository.save(quotaSeat);
    }

    @Transactional
    public void deleteQuotaSeat(Long id) {
        quotaSeatRepository.deleteById(id);
    }
}
