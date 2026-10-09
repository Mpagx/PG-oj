/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { JudgeConfig } from './JudgeConfig';
import type { UserVO } from './UserVO';
export type QuestionVO = {
    acceptedNum?: number;
    content?: string;
    createTime?: string;
    difficulty?: string;
    id?: number;
    judgeConfig?: JudgeConfig;
    license?: string;
    packageType?: string;
    source?: string;
    sourceUrl?: string;
    submitNum?: number;
    status?: string;
    tags?: Array<string>;
    title?: string;
    updateTime?: string;
    userId?: number;
    userVO?: UserVO;
};

