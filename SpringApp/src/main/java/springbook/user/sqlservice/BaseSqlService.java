package springbook.user.sqlservice;

import javax.annotation.PostConstruct;

public class BaseSqlService implements SqlService {
	// BaseSqlService는 상속을 통해 확장해서 사용하기에 적합하다. 서브 클래스에서 필요한 경우 접근할 수 있도록 protected로 선언한다.
	protected SqlReader sqlReader;
	protected SqlRegistry sqlRegistry;

	public void setSqlReader(SqlReader sqlReader) {
		this.sqlReader = sqlReader;
	}

	public void setSqlRegistry(SqlRegistry sqlRegistry) {
		this.sqlRegistry = sqlRegistry;
	}

	@PostConstruct
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

}
