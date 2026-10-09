/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { JudgeCase } from './JudgeCase';
import type { JudgeConfig } from './JudgeConfig';
export type QuestionUpdateRequest = {
    acceptedNum?: number;
    answer?: string;
    content?: string;
    difficulty?: string;
    id?: number;
    judgeCase?: Array<JudgeCase>;
    judgeConfig?: JudgeConfig;
    submitNum?: number;
    status?: string;
    tags?: Array<string>;
    title?: string;
};

