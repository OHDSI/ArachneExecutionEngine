SELECT concept_id, concept_name, concept_level, concept_class, vocabulary_id, concept_code, valid_start_date, valid_end_date, invalid_reason
FROM @cdm_schema.concept;
SELECT ancestor_concept_id, descendant_concept_id, min_levels_of_separation, max_levels_of_separation
FROM @cdm_schema.concept_ancestor;
SELECT concept_id_1, concept_id_2, relationship_id, valid_start_date, valid_end_date, invalid_reason
FROM @cdm_schema.concept_relationship;
SELECT concept_synonym_id, concept_id, concept_synonym_name
FROM @cdm_schema.concept_synonym;
SELECT drug_concept_id, ingredient_concept_id, amount_value, amount_unit, concentration_value, concentration_enum_unit, concentration_denom_unit, valid_start_date, valid_end_date, invalid_reason
FROM @cdm_schema.drug_strength;
SELECT relationship_id, relationship_name, is_hierarchical, defines_ancestry, reverse_relationship
FROM @cdm_schema.relationship;
SELECT source_code, source_vocabulary_id, source_code_description, target_concept_id, target_vocabulary_id, mapping_type, primary_map, valid_start_date, valid_end_date, invalid_reason
FROM @cdm_schema.source_to_concept_map;
SELECT vocabulary_id, vocabulary_name
FROM @cdm_schema.vocabulary;
