package springbook.learningtest.jdk.jaxb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.junit.Test;

import springbook.user.sqlservice.jaxb.SqlType;
import springbook.user.sqlservice.jaxb.Sqlmap;

public class JaxbTest {
	@Test
	public void readSqlmap() throws JAXBException, IOException{
		String contextPath = Sqlmap.class.getPackage().getName();
		// 바인딩용 클래스들 위치를 가지고 JAXB 컨텍스트를 만든다.
		JAXBContext context = JAXBContext.newInstance(contextPath);
		// 언마샬러 생성
		Unmarshaller unmarshaller = context.createUnmarshaller();
		// 언마샬을 하면 매핑된 오브젝트 트리의 루트인 Sqlmap을 돌려준다.
		Sqlmap sqlmap = (Sqlmap)unmarshaller.unmarshal(getClass().getResourceAsStream("sqlmap.xml"));
		
		List<SqlType> sqlList = sqlmap.getSql();
		
		assertThat(sqlList.size(), is(3));
	}

}
