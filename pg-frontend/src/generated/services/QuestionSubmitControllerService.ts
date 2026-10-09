/* generated using openapi-typescript-codegen -- do not edit */
/* istanbul ignore file */
/* tslint:disable */
/* eslint-disable */
import type { BaseResponse_long_ } from "../models/BaseResponse_long_";
import type { BaseResponse_Page_QuestionSubmitVO_ } from "../models/BaseResponse_Page_QuestionSubmitVO_";
import type { BaseResponse_QuestionSubmitVO_ } from "../models/BaseResponse_QuestionSubmitVO_";
import type { BaseResponse_UserSubmissionOverviewVO_ } from "../models/BaseResponse_UserSubmissionOverviewVO_";
import type { QuestionSubmitAddRequest } from "../models/QuestionSubmitAddRequest";
import type { QuestionSubmitQueryRequest } from "../models/QuestionSubmitQueryRequest";
import type { CustomTestRequest } from "../models/CustomTestRequest";
import type { BaseResponse_CustomTestResultVO_ } from "../models/BaseResponse_CustomTestResultVO_";
import type { CancelablePromise } from "../core/CancelablePromise";
import { OpenAPI } from "../core/OpenAPI";
import { request as __request } from "../core/request";
export class QuestionSubmitControllerService {
  public static runCustomTestUsingPost(
    customTestRequest: CustomTestRequest
  ): CancelablePromise<BaseResponse_CustomTestResultVO_ | any> {
    return __request(OpenAPI, {
      method: "POST",
      url: "/api/question_submit/custom-test",
      body: customTestRequest,
      errors: { 401: `Unauthorized`, 403: `Forbidden`, 429: `Too Many Requests` },
    });
  }
  public static getMySubmissionOverviewUsingGet(): CancelablePromise<
    BaseResponse_UserSubmissionOverviewVO_ | any
  > {
    return __request(OpenAPI, {
      method: "GET",
      url: "/api/question_submit/my/overview",
      errors: { 401: `Unauthorized`, 403: `Forbidden` },
    });
  }

  public static listMyQuestionSubmissionsUsingPost(
    questionSubmitQueryRequest: QuestionSubmitQueryRequest
  ): CancelablePromise<BaseResponse_Page_QuestionSubmitVO_ | any> {
    return __request(OpenAPI, {
      method: "POST",
      url: "/api/question_submit/my/list/page",
      body: questionSubmitQueryRequest,
      errors: { 401: `Unauthorized`, 403: `Forbidden` },
    });
  }
  /**
   * getQuestionSubmitById
   * @param id id
   * @returns BaseResponse_QuestionSubmitVO_ OK
   * @throws ApiError
   */
  public static getQuestionSubmitByIdUsingGet(
    id: number
  ): CancelablePromise<BaseResponse_QuestionSubmitVO_ | any> {
    return __request(OpenAPI, {
      method: "GET",
      url: "/api/question_submit/get",
      query: {
        id: id,
      },
      errors: {
        401: `Unauthorized`,
        403: `Forbidden`,
        404: `Not Found`,
      },
    });
  }
  /**
   * doQuestionSubmit
   * @param questionSubmitAddRequest questionSubmitAddRequest
   * @returns BaseResponse_long_ OK
   * @returns any Created
   * @throws ApiError
   */
  public static doQuestionSubmitUsingPost(
    questionSubmitAddRequest: QuestionSubmitAddRequest
  ): CancelablePromise<BaseResponse_long_ | any> {
    return __request(OpenAPI, {
      method: "POST",
      url: "/api/question_submit/",
      body: questionSubmitAddRequest,
      errors: {
        401: `Unauthorized`,
        403: `Forbidden`,
        404: `Not Found`,
      },
    });
  }
  /**
   * listQuestionSubmitByPage
   * @param questionSubmitQueryRequest questionSubmitQueryRequest
   * @returns BaseResponse_Page_QuestionSubmitVO_ OK
   * @returns any Created
   * @throws ApiError
   */
  public static listQuestionSubmitByPageUsingPost(
    questionSubmitQueryRequest: QuestionSubmitQueryRequest
  ): CancelablePromise<BaseResponse_Page_QuestionSubmitVO_ | any> {
    return __request(OpenAPI, {
      method: "POST",
      url: "/api/question_submit/list/page",
      body: questionSubmitQueryRequest,
      errors: {
        401: `Unauthorized`,
        403: `Forbidden`,
        404: `Not Found`,
      },
    });
  }
}
