package springbook.user.sqlservice;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import springbook.user.dao.UserDao;
import springbook.user.sqlservice.jaxb.SqlType;
import springbook.user.sqlservice.jaxb.Sqlmap;

public class XmlSqlService implements SqlService, SqlRegistry,  SqlReader {
	private SqlReader sqlReader;
	private SqlRegistry sqlRegistry;

	public void setSqlReader(SqlReader sqlReader) {
		this.sqlReader = sqlReader;
	}

	public void setSqlRegistry(SqlRegistry sqlRegistry) {
		this.sqlRegistry = sqlRegistry;
	}

	// SqlRegistry용
	private Map<String, String> sqlMap = new HashMap<String, String>();
	
	// SqlReader용
	private String sqlmapFile;
	public void setSqlmapFile(String sqlmapFile) {
		this.sqlmapFile = sqlmapFile;
	}
	
	@PostConstruct // loadSql() 메소드를 빈의 초기화 메소드로 지정한다.
	public void loadSql() {
		this.sqlReader.read(this.sqlRegistry);
	}
	
	@Override
	public String getSql(String key) throws SqlRetrievalFailureException {
		try {
			return this.sqlRegistry.findSql(key);
		}catch(SqlNotFoundException e) {
			throw new SqlRetrievalFailureException(e);
		}
	}

	@Override
	public void registerSql(String key, String sql) {
		sqlMap.put(key, sql);
	}

	@Override // HashMap이라는 저장소를 사용하는 구체적인 구현 방법에서 독립될 수 있도록 인터페이스의 메소드로 접근하게 해준다.
	public String findSql(String key) throws SqlNotFoundException {
		String sql = sqlMap.get(key);
		if(sql == null) throw new SqlRetrievalFailureException(key + "를 이용해서 SQL를 찾을 수 없습니다");
		else return sql;
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
