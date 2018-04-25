package springbook.user.sqlservice;

import java.io.IOException;

import javax.annotation.PostConstruct;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.oxm.Unmarshaller;

import springbook.user.dao.UserDao;
import springbook.user.sqlservice.jaxb.SqlType;
import springbook.user.sqlservice.jaxb.Sqlmap;

public class OxmSqlService implements SqlService {
	private final BaseSqlService baseSqlService = new BaseSqlService();
	
	// final이므로 변경 불가능하다. OxmSqlService와 OxmSqlReader는 강하게 결합돼서 하나의 빈으로 등록되고 한 번에 설정할 수 있다.
	private final OxmSqlReader oxmSqlReader = new OxmSqlReader();
	// oxmSqlReader와 달리 단지 디폴트 오브젝트로 만들어진 프로퍼티. 따라서 필요에 따라 DI를 통해 교체가 가능.
	private SqlRegistry sqlRegistry = new HashMapSqlRegistry();
	private Unmarshaller unmarshaller;
	
	public void setSqlRegistry(SqlRegistry sqlRegistry) {
		this.sqlRegistry = sqlRegistry;
	}
	
	// OxmSqlService의 공개된 프로퍼티를 통해 DI받은것을 그대로 멤버 클래스의 오브젝트에 전달한다. 이 setter들은 단일 빈 설정 구조를 위한 창구 역할을 할 뿐이다.
	public void setUnmarshaller(Unmarshaller unmarshaller) {
		this.oxmSqlReader.setUnmarshaller(unmarshaller);
	}
	
	public void setSqlmap(Resource sqlmap) {
		this.oxmSqlReader.setSqlmap(sqlmap);
	}
	
	@PostConstruct
	public void loadSql() {
		this.baseSqlService.setSqlReader(this.oxmSqlReader);
		this.baseSqlService.setSqlRegistry(this.sqlRegistry);
	
		// SQL을 등록하는 초기화 작업을 baseSqlService에 위임한다.
		this.baseSqlService.loadSql();
	}
	
	@Override
	public String getSql(String key) throws SqlRetrievalFailureException {
		return this.baseSqlService.getSql(key);
	}

	// private 멤버 클래스로 정의한다. 톱레벨 클래스인 OxmSqlService만이 사용할 수 있다.
	private class OxmSqlReader implements SqlReader{
		private Unmarshaller unmarshaller;
		private Resource sqlmap = new ClassPathResource("sqlmap.xml", UserDao.class);
		
		public void setUnmarshaller(Unmarshaller unmarshaller) {
			this.unmarshaller = unmarshaller;
		}

		public void setSqlmap(Resource sqlmap) {
			this.sqlmap = sqlmap;
		}
		
		@Override
		public void read(SqlRegistry sqlRegistry){
			try{
				Source source = new StreamSource(sqlmap.getInputStream());
				Sqlmap sqlmap = (Sqlmap)this.unmarshaller.unmarshal(source);
				for(SqlType sql : sqlmap.getSql())
					sqlRegistry.registerSql(sql.getKey(), sql.getValue());
			}catch(IOException e){
				throw new IllegalArgumentException(this.sqlmap.getFilename() + "을 가져올 수 없습니다", e);
			}
		}
	}
}
