package org.refit.refitbackend.global.swagger.spec.master;

import org.refit.refitbackend.domain.master.dto.MasterRes;
import org.refit.refitbackend.global.swagger.annotation.SwaggerApiSuccess;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public final class MasterSwaggerSpec {

    private MasterSwaggerSpec() {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "직무 목록 조회",
            operationDescription = "회원가입 및 필터링에 사용되는 전체 직무 목록을 조회합니다.",
            implementation = MasterRes.Jobs.class,
            responseDescription = "직무 목록 조회 성공"
    )
    public @interface Jobs {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "경력 레벨 목록 조회",
            operationDescription = "선택 가능한 전체 경력 레벨 목록을 조회합니다.",
            implementation = MasterRes.CareerLevels.class,
            responseDescription = "경력 레벨 목록 조회 성공"
    )
    public @interface CareerLevels {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "스킬 목록 조회",
            operationDescription = """
                    선택 가능한 전체 기술 스택 목록을 조회합니다.

                    - keyword 파라미터가 없으면 전체 조회
                    - keyword가 있으면 이름 기준 부분 검색
                    """,
            implementation = MasterRes.Skills.class,
            responseDescription = "스킬 목록 조회 성공"
    )
    public @interface Skills {}

    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @SwaggerApiSuccess(
            summary = "이메일 도메인 목록 조회",
            operationDescription = """
                    허용된 회사 이메일 도메인 목록을 조회합니다.

                    - cursor: 마지막 도메인 값
                    - size: 페이지 사이즈 (default 20)
                    """,
            implementation = MasterRes.EmailDomains.class,
            responseDescription = "이메일 도메인 목록 조회 성공"
    )
    public @interface EmailDomains {}
}
