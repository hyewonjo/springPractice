package springbook.user.sqlservice;

import java.io.InputStream;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import springbook.user.dao.UserDao;
import springbook.user.sqlservice.jaxb.SqlType;
import springbook.user.sqlservice.jaxb.Sqlmap;

public class JaxbXmlSqlReader implements SqlReader {
	private static final String DEFAULT_SQLMAP_FILE = "sqlmap.xml";
	
	//sqlmapFile은 SqlReader의 특정 구현 방법에 종속되는 프로퍼티가 된다.
	private String sqlmapFile = DEFAULT_SQLMAP_FILE;
	
	// sqlmapFile 프로퍼티를 지정하면 지정된 파일이 사용되고, 아니라면 이폴트로 넣은 파일이 사용된다.
	public void setSqlmapFile(String sqlmapFile) {
		this.sqlmapFile = sqlmapFile;
	}
	
	@Override
	public void read(SqlRegistry sqlRegistry) {
		// JAXB API를 이용해 XML 문서를 오브젝트 트리로 읽어온다.
		String contextPath = Sqlmap.class.getPackage().getName();
		try {
			JAXBContext context = JAXBContext.newInstance(contextPath);
			Unmarshaller unmarshaller = context.createUnmarshaller();
			InputStream is = UserDao.class.getResourceAsStream(sqlmapFile);
			Sqlmap sqlmap = (Sqlmap)unmarshaller.unmarshal(is);
			// 읽어온 SQL을 맵으로 저장해준다.
			for(SqlType sql : sqlmap.getSql()) {
				// SQL 저장 로직 구현에 독립적인 인터페이스 메소드를 통해 읽어들인 SQL과 키를 전달한다.
				sqlRegistry.registerSql(sql.getKey(), sql.getValue());
			}
		}catch(JAXBException e) {
			// JAXBException은 복구 불가능한 예외다. 불필요한 throws를 피하도록 런타임 예외로 포장해서 던진다.
			throw new RuntimeException(e);
		}
	}

}
