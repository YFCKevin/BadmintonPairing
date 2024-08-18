package com.yfckevin.badmintonPairing.service;

import com.yfckevin.badmintonPairing.entity.TemplateDetail;
import com.yfckevin.badmintonPairing.repository.TemplateDetailRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class TemplateDetailServiceImpl implements TemplateDetailService{
    private final TemplateDetailRepository templateDetailRepository;

    public TemplateDetailServiceImpl(TemplateDetailRepository templateDetailRepository) {
        this.templateDetailRepository = templateDetailRepository;
    }

    @Override
    public TemplateDetail save(TemplateDetail templateDetail) {
        return templateDetailRepository.save(templateDetail);
    }

    @Override
    public List<TemplateDetail> findByIdIn(List<String> detailIds) {
        return templateDetailRepository.findByIdIn(detailIds);
    }

    @Override
    public Optional<TemplateDetail> findById(String detailId) {
        return templateDetailRepository.findById(detailId);
    }
}
