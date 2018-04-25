package springbook.user.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static springbook.user.service.UserServiceImpl.MIN_LOGCOUNT_FOR_SILVER;
import static springbook.user.service.UserServiceImpl.MIN_RECOMMEND_FOR_GOLD;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.DataSource;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;

import springbook.user.dao.UserDao;
import springbook.user.domain.Level;
import springbook.user.domain.User;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="/applicationContext.xml")
public class UserServiceTest {
	@Autowired UserService userService;
	@Autowired UserDao userDao;
	@Autowired DataSource dataSource;
	@Autowired PlatformTransactionManager transactionManager;
	@Autowired MailSender mailSender;
	@Autowired UserService userServiceImpl;
	@Autowired ApplicationContext context; // 팩토리 빈을 가져오려면 애플리케이션 컨텍스트가 필요하다.
	@Autowired UserService testUserService;
	
	List<User> users;
	
	static class TestUserServiceException extends RuntimeException{}

	static class TestUserService extends UserServiceImpl{
		private String id = "madnite1";
		
		@Override
		protected void upgradeLevel(User user) {
			if(user.getId().equals(this.id)) throw new TestUserServiceException();
			super.upgradeLevel(user);
		}
		
		@Override
		public List<User> getAll(){
			for(User user : super.getAll())
				super.update(user);
			return null;
		}
	}
	
	static class MockMailSender implements MailSender{
		
		private List<String> requests = new ArrayList<String>();
		
		public List<String> getRequest(){
			return requests;
		}

		@Override
		public void send(SimpleMailMessage simpleMessage) throws MailException {
			requests.add(simpleMessage.getTo()[0]);
			
		}

		@Override
		public void send(SimpleMailMessage... simpleMessages) throws MailException {
			// TODO Auto-generated method stub
			
		}
	}
	
	static class MockUserDao implements UserDao{
		private List<User> users;
		private List<User> updated = new ArrayList();
		
		private MockUserDao(List<User> users) {
			this.users = users;
		}
		
		public List<User> getUpdated(){
			return this.updated;
		}
		
		@Override
		public List<User> getAll() { // 스텁 기능 제공
			return this.users;
		}
		
		@Override
		public void update(User user) { // 목 오브젝트 기능 제공
			updated.add(user);
		}
		
		@Override
		public void add(User user) { throw new UnsupportedOperationException();}
		@Override
		public User get(String id) { throw new UnsupportedOperationException();}
		@Override
		public void deleteAll() { throw new UnsupportedOperationException();}
		@Override
		public int getCount() { throw new UnsupportedOperationException();}
	}
	
	@Before
	public void setUp() {
		users = Arrays.asList(
				new User("bumjin", "박범진", "p1", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER-1, 0, "hyewonjo92@gmail.com"),
				new User("joytouch", "강명성", "p2", Level.BASIC, MIN_LOGCOUNT_FOR_SILVER, 0, "hyewonjo92@gmail.com"),
				new User("erwins", "신승한", "p3", Level.SILVER, 60, MIN_RECOMMEND_FOR_GOLD-1, "hyewonjo92@gmail.com"),
				new User("madnite1", "이상호", "p4", Level.SILVER, 60, MIN_RECOMMEND_FOR_GOLD, "hyewonjo92@gmail.com"),
				new User("green", "오민규", "p5", Level.GOLD, 100, Integer.MAX_VALUE, "hyewonjo92@gmail.com")
		);
	}
	
	@Test
	public void bean() {
		assertThat(this.userService, is(notNullValue()));
	}
	
	@Test
	@DirtiesContext // 컨텍스트의 DI 설정을 변경하는 테스트라는 것을 알려줌.
	public void upgradeLevels() throws SQLException {
		// 고립된 테스트에서는 테스트 대상 오브젝트를 직접 생성하면 됨.
		UserServiceImpl userServiceImpl = new UserServiceImpl();
		
		MockUserDao mockUserDao = new MockUserDao(this.users);
		userServiceImpl.setUserDao(mockUserDao);
		
		MockMailSender mockMailSender = new MockMailSender();
		userServiceImpl.setMailSender(mockMailSender);
		
		userServiceImpl.upgradeLevels();
		
		List<User> updated = mockUserDao.getUpdated();
		assertThat(updated.size(), is(2));
		checkUserAndLevel(updated.get(0), "joytouch", Level.SILVER);
		checkUserAndLevel(updated.get(1), "madnite1", Level.GOLD);
		
		List<String> request = mockMailSender.getRequest();
		assertThat(request.size(), is(2));
		assertThat(request.get(0), is(users.get(1).getEmail()));
		assertThat(request.get(1), is(users.get(3).getEmail()));
	}
	
	@Test
	public void mockUpgradeLevels() {
		UserServiceImpl userServiceImpl = new UserServiceImpl();
		// 인터페이스를 이용해 mock을 만든다.
		UserDao mockUserDao = mock(UserDao.class);
		when(mockUserDao.getAll()).thenReturn(this.users);
		userServiceImpl.setUserDao(mockUserDao);
		
		MailSender mockMailSender = mock(MailSender.class);
		userServiceImpl.setMailSender(mockMailSender);
		
		userServiceImpl.upgradeLevels();
		// 목 오브젝트가 제공하는 검증기능을 통해서 어떤 메소드가 몇번 호출됐는지, 파라미터는 무엇인지 확인할 수 있음.
		verify(mockUserDao, times(2)).update(any(User.class));
		verify(mockUserDao, times(2)).update(any(User.class));
		verify(mockUserDao).update(users.get(1));
		assertThat(users.get(1).getLevel(), is(Level.SILVER));
		verify(mockUserDao).update(users.get(3));
		assertThat(users.get(3).getLevel(), is(Level.GOLD));
		
		ArgumentCaptor<SimpleMailMessage> mailMessageArg = ArgumentCaptor.forClass(SimpleMailMessage.class);
		// 파라미터를 정밀하게 검사하기위해 캡쳐할 수도 있다.
		verify(mockMailSender, times(2)).send(mailMessageArg.capture());
		List<SimpleMailMessage> mailMessages = mailMessageArg.getAllValues();
		assertThat(mailMessages.get(0).getTo()[0], is(users.get(1).getEmail()));
		assertThat(mailMessages.get(1).getTo()[0], is(users.get(3).getEmail()));
	}
	
	private void checkLevel(User user, Level expectedLevel) {
		User userUpdate = userDao.get(user.getId());
		assertThat(userUpdate.getLevel(), is(expectedLevel));
	}
	
	private void checkLevelUpgraded(User user, boolean upgraded) {
		User userUpdate = userDao.get(user.getId());
		if(upgraded)
			assertThat(userUpdate.getLevel(), is(user.getLevel().nextLevel()));
		else
			assertThat(userUpdate.getLevel(), is(user.getLevel()));
	}
	
	private void checkUserAndLevel(User updated, String expectedId, Level expectedLevel) {
		assertThat(updated.getId(), is(expectedId));
		assertThat(updated.getLevel(), is(expectedLevel));
	}
	
	@Test
	public void add() {
		userDao.deleteAll();
		
		User userWithLevel = users.get(4);
		User userWithoutLevel = users.get(0);
		userWithoutLevel.setLevel(null);
		
		userService.add(userWithLevel);
		userService.add(userWithoutLevel);
		
		User userWithLevelRead = userDao.get(userWithLevel.getId());
		User usetWithoutLevelRead = userDao.get(userWithoutLevel.getId());
		
		assertThat(userWithLevelRead.getLevel(), is(userWithLevel.getLevel()));
		assertThat(usetWithoutLevelRead.getLevel(), is(Level.BASIC));	
	}
	
	@Test
	public void upgradeAllOrNothing() throws Exception {
		userDao.deleteAll();
		for(User user : users) userDao.add(user);
		
		try {
			this.testUserService.upgradeLevels();
			fail("TestUserServiceException expected");
		}catch(TestUserServiceException e) {}
		
		checkLevelUpgraded(users.get(1), false);
	}
	
	@Test
	public void advisorAutoProxyCreator() {
		assertEquals(testUserService, java.lang.reflect.Proxy.class);
	}
	
	@Test(expected=TransientDataAccessResourceException.class)
	public void readOnlyTransactionAttribute() {
		testUserService.getAll();
	}
}
