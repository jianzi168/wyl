package com.form.cartesianv2.service;

import com.form.cartesianv2.dto.FormCellDTO;
import com.form.cartesianv2.dto.FormFieldDTO;

import java.util.List;

public interface ICartesianBuilderService {
    List<List<FormCellDTO>> buildCrossProduct(List<List<FormFieldDTO>> rowFieldGroups, List<List<FormFieldDTO>> colFieldGroups);
}
