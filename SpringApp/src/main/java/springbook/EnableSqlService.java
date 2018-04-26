package springbook;

import org.springframework.context.annotation.Import;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Import(value=SqlServiceContext.class)
@Retention(RetentionPolicy.RUNTIME) //책의 실습 예제에는 이 부분이 없다. 이부분을 제외 시키면 빈생성중에 에러가 발생한다.
public @interface EnableSqlService {
	// Retention은 어노테이션의 범위(?)라고 할 수 있겠습니다. 어떤 시점까지 어노테이션이 영향을 미치는지 결정합니다.
	// @Retention(RetentionPolicy.RUNTIME) // 컴파일 이후에도 JVM에 의해서 참조가 가능합니다.
	//@Retention(RetentionPolicy.CLASS) // 컴파일러가 클래스를 참조할 때까지 유효합니다.
	//@Retention(RetentionPolicy.SOURCE) // 어노테이션 정보는 컴파일 이후 없어집니다.
}
