import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

class Main {
	public static void main(String[] args) {
		App app = new App();
		app.start();
	}
}

// Session
// 현재 사용자가 이용중인 정보
// 이 안의 정보는 사용자가 프로그램을 사용할 때 동안은 계속 유지된다.
class Session {
	private Member loginedMember;
	private Board currentBoard;

	public Member getLoginedMember() {
		return loginedMember;
	}

	public void setLoginedMember(Member loginedMember) {
		this.loginedMember = loginedMember;
	}

	public Board getCurrentBoard() {
		return currentBoard;
	}

	public void setCurrentBoard(Board currentBoard) {
		this.currentBoard = currentBoard;
	}

	public boolean isLogined() {
		return loginedMember != null;
	}
}

// Factory
// 프로그램 전체에서 공유되는 객체 리모콘을 보관하는 클래스

class Factory {
	private static Session session;
	private static DB db;
	private static BuildService buildService;
	private static ArticleService articleService;
	private static ArticleDao articleDao;
	private static MemberService memberService;
	private static MemberDao memberDao;
	private static Scanner scanner;

	public static Session getSession() {
		if (session == null) {
			session = new Session();
		}

		return session;
	}

	public static Scanner getScanner() {
		if (scanner == null) {
			scanner = new Scanner(System.in);
		}

		return scanner;
	}

	public static DB getDB() {
		if (db == null) {
			db = new DB();
		}

		return db;
	}

	public static ArticleService getArticleService() {
		if (articleService == null) {
			articleService = new ArticleService();
		}

		return articleService;
	}

	public static ArticleDao getArticleDao() {
		if (articleDao == null) {
			articleDao = new ArticleDao();
		}

		return articleDao;
	}

	public static MemberService getMemberService() {
		if (memberService == null) {
			memberService = new MemberService();
		}
		return memberService;
	}

	public static MemberDao getMemberDao() {
		if (memberDao == null) {
			memberDao = new MemberDao();
		}

		return memberDao;
	}

	public static BuildService getBuildService() {
		if (buildService == null) {
			buildService = new BuildService();
		}

		return buildService;
	}
}

// App
class App {
	private Map<String, Controller> controllers;

	// 컨트롤러 만들고 한곳에 정리
	// 나중에 컨트롤러 이름으로 쉽게 찾아쓸 수 있게 하려고 Map 사용
	void initControllers() {
		controllers = new HashMap<>();
		controllers.put("build", new BuildController());
		controllers.put("article", new ArticleController());
		controllers.put("member", new MemberController());
	}

	public App() {
		// 컨트롤러 등록
		initControllers();

		// 관리자 회원 생성
		Factory.getMemberService().join("admin", "admin", "관리자");

		// 공지사항 게시판 생성
		Factory.getArticleService().makeBoard("공지시항", "notice");
		// 자유 게시판 생성
		Factory.getArticleService().makeBoard("자유게시판", "free");

		// 현재 게시판을 1번 게시판으로 선택
		Factory.getSession().setCurrentBoard(Factory.getArticleService().getBoard(1));
		// 임시 : 현재 로그인 된 회원은 1번 회원으로 지정, 이건 나중에 회원가입, 로그인 추가되면 제거해야함
		Factory.getSession().setLoginedMember(Factory.getMemberService().getMember(1));
	}

	public void start() {

		while (true) {
			System.out.printf("명령어 : ");
			String command = Factory.getScanner().nextLine().trim();

			if (command.length() == 0) {
				continue;
			} else if (command.equals("exit")) {
				break;
			}

			Request reqeust = new Request(command);

			if (reqeust.isValidRequest() == false) {
				continue;
			}

			if (controllers.containsKey(reqeust.getControllerName()) == false) {
				continue;
			}

			controllers.get(reqeust.getControllerName()).doAction(reqeust);
		}

		Factory.getScanner().close();
	}
}

// Request
class Request {
	private String requestStr;
	private String controllerName;
	private String actionName;
	private String arg1;
	private String arg2;
	private String arg3;

	boolean isValidRequest() {
		return actionName != null;
	}

	Request(String requestStr) {
		this.requestStr = requestStr;
		String[] requestStrBits = requestStr.split(" ");
		this.controllerName = requestStrBits[0];

		if (requestStrBits.length > 1) {
			this.actionName = requestStrBits[1];
		}

		if (requestStrBits.length > 2) {
			this.arg1 = requestStrBits[2];
		}

		if (requestStrBits.length > 3) {
			this.arg2 = requestStrBits[3];
		}

		if (requestStrBits.length > 4) {
			this.arg3 = requestStrBits[4];
		}
	}

	public String getControllerName() {
		return controllerName;
	}

	public void setControllerName(String controllerName) {
		this.controllerName = controllerName;
	}

	public String getActionName() {
		return actionName;
	}

	public void setActionName(String actionName) {
		this.actionName = actionName;
	}

	public String getArg1() {
		return arg1;
	}

	public void setArg1(String arg1) {
		this.arg1 = arg1;
	}

	public String getArg2() {
		return arg2;
	}

	public void setArg2(String arg2) {
		this.arg2 = arg2;
	}

	public String getArg3() {
		return arg3;
	}

	public void setArg3(String arg3) {
		this.arg3 = arg3;
	}
}

// Controller
abstract class Controller {
	abstract void doAction(Request reqeust);
}

class ArticleController extends Controller {
	private ArticleService articleService;

	ArticleController() {
		articleService = Factory.getArticleService();
	}

	public void doAction(Request reqeust) {
		if (reqeust.getActionName().equals("list")) {
			actionList(reqeust);
		} else if (reqeust.getActionName().equals("write")) {
			actionWrite(reqeust);
		} else if (reqeust.getActionName().equals("createBoard")) {
			actionCreateBoard(reqeust);
		} else if (reqeust.getActionName().equals("deleteBoard")) {
			actionDeleteBoard(reqeust);
		} else if (reqeust.getActionName().equals("changeBoard")) {
			actionChangeBoard(reqeust);
		} else if (reqeust.getActionName().equals("modify")) {
			actionModify(reqeust);
		} else if (reqeust.getActionName().equals("delete")) {
			actionDelete(reqeust);
		} else if (reqeust.getActionName().equals("detail")) {
			actionDetail(reqeust);
		}
	}

	private void actionDetail(Request reqeust) {
		System.out.println("== 게시물 상세 ==");
		int id = Integer.parseInt(reqeust.getArg1());

		Article article = articleService.getArticle(id);

		if (article == null) {
			System.out.println("없는 게시물");
		} else {
			System.out.println(article.toString());
		}
	}

	// 게시물 삭제
	private void actionDelete(Request reqeust) {
		if(Factory.getSession().getLoginedMember() != null) {
			System.out.println("== 게시물 삭제 ==");
			int id = Integer.parseInt(reqeust.getArg1());

			// 현재 게시판 id 가져오기
			int boardId = Factory.getSession().getCurrentBoard().getId();

			// 현재 로그인한 회원의 id 가져오기
			int memberId = Factory.getSession().getLoginedMember().getId();

			int isId = articleService.articleDelete(boardId, memberId, id);

			if (isId == -1) {
				System.out.println("없는 게시물");
			} else if (isId == -2) {
				System.out.println("해당 게시판 접속 필요");
			} else if (isId == -3) {
				System.out.println("본인 게시물 아님");
			} else {
				System.out.printf("%d번 게시물 삭제 완료\n", isId);
			}
		} else {
			System.out.println("로그아웃 상태입니다");
		}
		
	}

	// 게시물 수정
	private void actionModify(Request reqeust) {
		if(Factory.getSession().getLoginedMember() != null) {
			System.out.println("== 게시물 수정 ==");

			int id = Integer.parseInt(reqeust.getArg1());
			String title = "";
			String body = "";

			while (true) {
				System.out.print("제목 : ");
				title = Factory.getScanner().nextLine();
				if (title.length() == 0) {
					continue;
				}

				break;
			}

			while (true) {
				System.out.print("내용 : ");
				body = Factory.getScanner().nextLine();
				if (body.length() == 0) {
					continue;
				}

				break;
			}

			// 현재 게시판 id 가져오기
			int boardId = Factory.getSession().getCurrentBoard().getId();

			// 현재 로그인한 회원의 id 가져오기
			int memberId = Factory.getSession().getLoginedMember().getId();

			int newId = articleService.modify(boardId, memberId, title, body, id);

			if (newId == -1) {
				System.out.println("없는 게시물");
			} else if (newId == -2) {
				System.out.println("해당 게시판 접속 필요");
			} else if (newId == -3) {
				System.out.println("본인 게시물 아님");
			} else {
				System.out.printf("%d번 게시물 수정 완료\n", newId);
			}

		} else {
			System.out.println("로그아웃 상태입니다");
		}
		
	}

	// 게시판 이동(변경)
	private void actionChangeBoard(Request reqeust) {
		String boardCode = reqeust.getArg1();

		Board isboard = Factory.getArticleService().moveBoard(boardCode);

		if (isboard == null) {
			System.out.println("존재하지 않는 게시판 입니다.");
		} else {
			Factory.getSession().setCurrentBoard(Factory.getArticleService().getBoard(isboard.getId()));
			System.out.printf("{%s}게시판으로 이동 하였습니다\n", isboard.getName());
		}
	}

	// 게시판 삭제하기 + 자동으로 공지사항 게시판으로 이동
	private void actionDeleteBoard(Request reqeust) {
		String boardCode = reqeust.getArg1();

		Board isboard = Factory.getArticleService().deleteBoard(boardCode);

		if (isboard == null) {
			System.out.println("존재하지 않는 게시판 CODE 입니다.");
		} else if (isboard.getId() == 1) {
			System.out.println("공지사항 게시판은 삭제할 수 없습니다.");
		} else {
			Factory.getSession().setLoginedMember(Factory.getMemberService().getMember(1));
			System.out.printf("{%s}게시판 삭제 성공\n", isboard.getName());
		}
	}

	// 게시판 만들기
	// 중복 name은 미설정
	private void actionCreateBoard(Request reqeust) {
		System.out.println("== 게시판 만들기 ==");
		System.out.printf("게시판명 : ");
		String boardName = Factory.getScanner().nextLine().trim();
		System.out.printf("게시판 CODE : ");
		String boardCode = Factory.getScanner().nextLine().trim();

		int isboardId = Factory.getArticleService().makeBoard(boardName, boardCode);

		if (isboardId == -1) {
			System.out.println("이미 존재하는 게시판 입니다.");
		} else {
			System.out.println("게시판 만들기 성공");
		}
	}

	private void actionList(Request reqeust) {
		String id = reqeust.getArg1();
		int id_ = 0;
		int count = 0;
		int count_ = 0;
		List<Article> articles = articleService.getArticles();

		if (id == null && articles != null) {
			for (Article article : articles) {
				System.out.println(article.toString());
			}
		} else if (id != null && articles != null){
			id_ = Integer.parseInt(id);
			System.out.println("========================================{" + id + "}page========================================");
			for (int j = 0; j < id_; j++) {
				for(int i = count; i < count+10; i++) {
					
					if(articles.size() <= i) {
						break;
					}
					
					if(j == id_-1) {
						System.out.println(articles.get(i).toString());
					}
					count_++;
				}
				count += count_;
			}
		}
		
//		검색 만들기 : 
//		1. arg2의 length()을 구한다
//		2. 해당 페이지에 있는 게시글들(10) 전체 반복
//		3. 제목(title)을 length()로 일일이 쪼갠다
//		4. 그중 arg2와 일치하는 것이 있으면 출력할 수 있도록 한다.
	}

	private void actionWrite(Request reqeust) {
		
		if(Factory.getSession().getLoginedMember() != null) {
			String title = "";
			String body = "";

			while (true) {
				System.out.print("제목 : ");
				title = Factory.getScanner().nextLine();
				if (title.length() == 0) {
					continue;
				}

				break;
			}

			while (true) {
				System.out.print("내용 : ");
				body = Factory.getScanner().nextLine();
				if (body.length() == 0) {
					continue;
				}

				break;
			}

			// 현재 게시판 id 가져오기
			int boardId = Factory.getSession().getCurrentBoard().getId();

			// 현재 로그인한 회원의 id 가져오기
			int memberId = Factory.getSession().getLoginedMember().getId();
			int newId = articleService.write(boardId, memberId, title, body);

			System.out.printf("%d번 글이 생성되었습니다.\n", newId);
		} else {
			System.out.println("로그아웃 상태입니다");
		}
		
	}
}

class BuildController extends Controller {
	private static BuildService buildService;

	BuildController() {
		buildService = Factory.getBuildService();
	}

	@Override
	void doAction(Request reqeust) {
		if (reqeust.getActionName().equals("site")) {
			actionSite(reqeust);
		} else if (reqeust.getActionName().equals("startAutoSite")) {
			actionStartAutoSite(reqeust);
		} else if (reqeust.getActionName().equals("stopAutoSite")) {
			actionStoptAutoSite(reqeust);
		}
	}

	private void actionStoptAutoSite(Request reqeust) {
		buildService.stopWork();
	}

	private void actionStartAutoSite(Request reqeust) {
		buildService.startWork();
	}

	private void actionSite(Request reqeust) {
		buildService.buildSite();
	}
}

class MemberController extends Controller {
	private MemberService memberService;

	MemberController() {
		memberService = Factory.getMemberService();
	}

	void doAction(Request reqeust) {
		if (reqeust.getActionName().equals("logout")) {
			actionLogout(reqeust);
		} else if (reqeust.getActionName().equals("login")) {
			actionLogin(reqeust);
		} else if (reqeust.getActionName().equals("whoami")) {
			actionWhoami(reqeust);
		} else if (reqeust.getActionName().equals("join")) {
			actionJoin(reqeust);
		}
	}

	private void actionJoin(Request reqeust) {
		System.out.printf("== 회원가입 시작 ==\n");

		String name;
		String loginId;
		String loginPw;
		String loginPwConfirm;

		while (true) {
			System.out.printf("이름 : ");
			name = Factory.getScanner().nextLine().trim();

			if (name.length() == 0) {
				System.out.printf("이름을 입력해주세요.\n");

				continue;
			}

			if (name.length() < 2) {
				System.out.printf("이름을 2자 이상 입력해주세요.\n");

				continue;
			}

			break;
		}

		while (true) {
			System.out.printf("로그인 아이디 : ");
			loginId = Factory.getScanner().nextLine().trim();

			if (loginId.length() == 0) {
				System.out.printf("로그인 아이디를 입력해주세요.\n");

				continue;
			}

			if (loginId.length() < 2) {
				System.out.printf("로그인 아이디를 2자 이상 입력해주세요.\n");

				continue;
			}

			if (memberService.isUsedLoginId(loginId)) {
				System.out.printf("입력하신 아이디(%s)는 이미 사용중 입니다.\n", loginId);

				continue;
			}

			break;
		}

		while (true) {
			boolean loginPwValid = true;

			while (true) {
				System.out.printf("로그인 비밀번호 : ");
				loginPw = Factory.getScanner().nextLine().trim();

				if (loginPw.length() == 0) {
					System.out.printf("로그인 비번을 입력해주세요.\n");

					continue;
				}

				if (loginPw.length() < 2) {
					System.out.printf("로그인 비번을 2자 이상 입력해주세요.\n");

					continue;
				}

				break;
			}

			while (true) {
				System.out.printf("로그인 비밀번호 확인 : ");
				loginPwConfirm = Factory.getScanner().nextLine().trim();

				if (loginPwConfirm.length() == 0) {
					System.out.printf("로그인 비번확인을 입력해주세요.\n");

					continue;
				}

				if (loginPw.equals(loginPwConfirm) == false) {
					System.out.printf("로그인 비번과 비번확인이 일치하지 않습니다.\n");
					loginPwValid = false;
					break;
				}

				break;
			}

			if (loginPwValid) {
				break;
			}
		}
		int rs = memberService.join(loginId, loginPw, name);
		Factory.getSession().setLoginedMember(Factory.getMemberService().getMember(rs));

		if (rs != -1) {
			System.out.println("성공하였습니다.");
		} else if (rs == -1) {
			System.out.println("입력하신 로그인 아이디는 이미 사용중입니다.");
		}
	}

	private void actionWhoami(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember == null) {
			System.out.println("나그네");
		} else {
			System.out.println(loginedMember.getName());
		}

	}

	private void actionLogin(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember == null) {
			System.out.printf("로그인 아이디 : ");
			String loginId = Factory.getScanner().nextLine().trim();

			System.out.printf("로그인 비번 : ");
			String loginPw = Factory.getScanner().nextLine().trim();

			Member member = memberService.getMemberByLoginIdAndLoginPw(loginId, loginPw);

			if (member == null) {
				System.out.println("일치하는 회원이 없습니다.");
			} else {
				System.out.println(member.getName() + "님 환영합니다.");
				Factory.getSession().setLoginedMember(member);
			}
		} else {
			System.out.printf("현재 %s님 로그인 상태입니다.\n", loginedMember.getName());
		}

	}

	private void actionLogout(Request reqeust) {
		Member loginedMember = Factory.getSession().getLoginedMember();

		if (loginedMember != null) {
			Session session = Factory.getSession();
			System.out.println("로그아웃 되었습니다.");
			session.setLoginedMember(null);
		}

	}
}

// Service
class BuildService {
	private static ArticleService articleService;
	private static boolean workStarted;
	private static MemberService memberService;

	BuildService() {
		articleService = Factory.getArticleService();
		memberService = Factory.getMemberService();
	}

	static {
		workStarted = false;
	}

	public void buildSite() {
		Util.makeDir("site");
		Util.makeDir("site/article");
		Util.makeDir("site/home");
		Util.makeDir("site/stat");
		Util.makeDir("site/resource");
		
		String css = Util.getFileContents("site_template/resource/common.css");
		String js = Util.getFileContents("site_template/resource/common.js");
		String log = Util.getFileContents("site_template/resource/log.html");
		String home = Util.getFileContents("site_template/home/index.html");
		String stat = Util.getFileContents("site_template/stat/index.html");
		Util.writeFileContents("site/resource/common.css", css);
		Util.writeFileContents("site/resource/common.js", js);
		Util.writeFileContents("site/resource/log.html", log);
		
		

		String head = Util.getFileContents("site_template/part/head.html");
		String foot = Util.getFileContents("site_template/part/foot.html");

		// 각 게시판 별 게시물리스트 페이지 생성
		List<Board> boards = articleService.getBoards();

		String middle = "";	
		for(Board board : boards) {
			String fileLink = board.getCode() + "-list-1.html";
			String fileName = board.getCode();
			middle += "<li><a href=\"../article/" + fileLink + "\">" + fileName + "</a></li>";
		}
		home = home.replace("${LI}", middle);
		stat = stat.replace("${LI}", middle);
		Util.writeFileContents("site/home/index.html", home);
		Util.writeFileContents("site/stat/index.html", stat);
		
		
		middle = "";	
		for(Board board : boards) {
			String fileLink = board.getCode() + "-list-1.html";
			String fileName = board.getCode();
			middle += "<li><a href=\"" + fileLink + "\">" + fileName + "</a></li>";
		}
//		
		for (Board board : boards) {
			head = Util.getFileContents("site_template/part/head.html");
			foot = Util.getFileContents("site_template/part/foot.html");
			String fileName = board.getCode() + "-list-1.html";

			String html = "";

			List<Article> articles = articleService.getArticlesByBoardCode(board.getCode());

			String template = Util.getFileContents("site_template/article/list.html");

			int count = 0;
			for (Article article : articles) {
				Member member = memberService.getMember(article.getMemberId());
				
				count++;
				html += "<tr>";
				html += "<td>" + count + "</td>";
//				html += "<td>" + article.getId() + "</td>";
				html += "<td>" + article.getRegDate() + "</td>";
				html += "<td>" +  member.getName() + "</td>";
				html += "<td><a href=\"" + article.getId() + ".html\">" + article.getTitle() + "</a></td>";
				html += "</tr>";
			}

			html = template.replace("${TR}", html);
			
			head = head.replace("${LI}", middle);
			
			html = head + html + foot;

			Util.writeFileContents("site/article/" + fileName, html);
		}

		// 게시물 별 파일 생성
		List<Article> articles = articleService.getArticles();

		int count = 0;
		for (Article article : articles) {
			String html = "";

			html += "<div>제목 : " + article.getTitle() + "</div>";
			html += "<div>내용 : " + article.getBody() + "</div>";
			html += "<div>작성자 : " + memberService.getMember(article.getMemberId()).getName() + "</div>";
			html += "<div><a href=\"" + (article.getId() - 1) + ".html\">이전글</a></div>";
			html += "<div><a href=\"" + (article.getId() + 1) + ".html\">다음글</a></div>";
			
			//이전페이지 다음페이지 없을경우.. 좀드릅지만 이렇게라도...
			if(count == 0) {
				html = "";
				html += "<div>제목 : " + article.getTitle() + "</div>";
				html += "<div>내용 : " + article.getBody() + "</div>";
				html += "<div>작성자 : " + memberService.getMember(article.getMemberId()).getName() + "</div>";
				html += "<div><a href=\"#\">이전글</a></div>";
				html += "<div><a href=\"" + (article.getId() + 1) + ".html\">다음글</a></div>";
			} else if(count == articles.size()-1) {
				html = "";
				html += "<div>제목 : " + article.getTitle() + "</div>";
				html += "<div>내용 : " + article.getBody() + "</div>";
				html += "<div>작성자 : " + memberService.getMember(article.getMemberId()).getName() + "</div>";
				html += "<div><a href=\"" + (article.getId() - 1) + ".html\">이전글</a></div>";
				html += "<div><a href=\"#\">다음글</a></div>";
			}
			count++;
			////

			html = head + html + foot;

			Util.writeFileContents("site/article/" + article.getId() + ".html", html);
			
		}
	}

	public void startWork() {
		workStarted = true;
		new Thread(() -> {
			while (workStarted) {
				try {
//					System.out.println("=build site=");
					buildSite();
					Thread.sleep(10000);
				} catch (InterruptedException e) {

				}
			}
		}).start();
	}

	public void stopWork() {
//		System.out.println("=build site Stop=");
		workStarted = false;
	}

}

class ArticleService {
	private ArticleDao articleDao;

	ArticleService() {
		articleDao = Factory.getArticleDao();
	}

	public Article getArticle(int id) {
		return articleDao.getArticle(id);
	}

	// 삭제 (admin은 안했다.)
	public int articleDelete(int boardId, int memberId, int id) {
		Article article = articleDao.getArticle(id);

		if (article == null) {
			return -1;
		}

		if (article.getBoardId() != boardId) {
			return -2;
		}

		if (article.getMemberId() != memberId) {
			return -3;
		}

		return articleDao.articleDelete(id);
	}

	// 수정 (admin은 안했다.)
	public int modify(int boardId, int memberId, String title, String body, int id) {
		Article article = articleDao.getArticle(id);

		if (article == null) {
			return -1;
		}

		if (article.getBoardId() != boardId) {
			return -2;
		}

		if (article.getMemberId() != memberId) {
			return -3;
		}

		Article modifyArticle = new Article(boardId, memberId, title, body);
		return articleDao.modifyArticle(modifyArticle, id);
	}

	public List<Article> getArticlesByBoardCode(String code) {
		return articleDao.getArticlesByBoardCode(code);
	}

	public List<Board> getBoards() {
		return articleDao.getBoards();
	}

	// 게시판 삭제
	public Board deleteBoard(String boardCode) {
		Board board = articleDao.getBoard(boardCode);

		if (board == null) {
			return null;
		}

		articleDao.deleteBoard(board.getId());
		return board;
	}

	// 게시판 이동
	public Board moveBoard(String boardCode) {
		Board board = articleDao.getBoard(boardCode);

		if (board == null) {
			return null;
		}

		return board;
	}

	// Id를 이용하여 Board를 받아온다
	public Board getBoard(int id) {
		return articleDao.getBoard(id);
	}

	// 게시판 만들기
	public int makeBoard(String name, String code) {
		Board oldBoard = articleDao.getBoardByCode(code);

		if (oldBoard != null) {
			return -1;
		}

		Board board = new Board(name, code);
		return articleDao.saveBoard(board);
	}

	// 게시글 쓰기
	public int write(int boardId, int memberId, String title, String body) {
		Article article = new Article(boardId, memberId, title, body);
		return articleDao.save(article);
	}

	public List<Article> getArticles() {
		return articleDao.getArticles();
	}

}

class MemberService {
	private MemberDao memberDao;

	MemberService() {
		memberDao = Factory.getMemberDao();
	}

	public boolean isUsedLoginId(String loginId) {
		Member member = memberDao.getMemberByLoginId(loginId);

		if (member == null) {
			return false;
		}

		return true;
	}

	public Member getMemberByLoginIdAndLoginPw(String loginId, String loginPw) {
		return memberDao.getMemberByLoginIdAndLoginPw(loginId, loginPw);
	}

	public int join(String loginId, String loginPw, String name) {
		Member oldMember = memberDao.getMemberByLoginId(loginId);

		if (oldMember != null) {
			return -1;
		}

		Member member = new Member(loginId, loginPw, name);
		return memberDao.save(member);
	}

	public Member getMember(int id) {
		return memberDao.getMember(id);
	}
}

// Dao
class ArticleDao {
	DB db;

	ArticleDao() {
		db = Factory.getDB();
	}

	// 게시물 삭제
	public int articleDelete(int id) {
		db.articleDelete(id);
		return id;
	}

	// 게시물 수정
	public int modifyArticle(Article modifyArticle, int id) {
		return db.modifyArticle(modifyArticle, id);
	}

	public Article getArticle(int articleId) {
		return db.getArticle(articleId);
	}

	// 게시판 삭제
	public void deleteBoard(int id) {
		db.deleteBoard(id);
	}

	// code로 board를 가져옴
	public Board getBoard(String boardCode) {
		return db.getBoard(boardCode);
	}

	// id로 board를 가져옴
	public Board getBoard(int id) {
		return db.getBoard(id);
	}

	public List<Article> getArticlesByBoardCode(String code) {
		return db.getArticlesByBoardCode(code);
	}

	public List<Board> getBoards() {
		return db.getBoards();
	}

	public Board getBoardByCode(String code) {
		return db.getBoardByCode(code);
	}

	public int saveBoard(Board board) {
		return db.saveBoard(board);
	}

	// 게시물 저장
	public int save(Article article) {
		return db.saveArticle(article);
	}

	public List<Article> getArticles() {
		return db.getArticles();
	}
}

class MemberDao {
	DB db;

	MemberDao() {
		db = Factory.getDB();
	}

	public Member getMemberByLoginIdAndLoginPw(String loginId, String loginPw) {
		return db.getMemberByLoginIdAndLoginPw(loginId, loginPw);
	}

	public Member getMemberByLoginId(String loginId) {
		return db.getMemberByLoginId(loginId);
	}

	public Member getMember(int id) {
		return db.getMember(id);
	}

	public int save(Member member) {
		return db.saveMember(member);
	}
}

// DB
class DB {
	private Map<String, Table> tables;

	public DB() {
		String dbDirPath = getDirPath();
		Util.makeDir(dbDirPath);

		tables = new HashMap<>();

		tables.put("article", new Table<Article>(Article.class, dbDirPath));
		tables.put("board", new Table<Board>(Board.class, dbDirPath));
		tables.put("member", new Table<Member>(Member.class, dbDirPath));
	}

	// 게시물 삭제
	public void articleDelete(int id) {
		tables.get("article").delete(id);

	}

	// 수정
	public int modifyArticle(Article modifyArticle, int id) {
		return tables.get("article").modify(modifyArticle, id);
	}

	public Article getArticle(int articleId) {
		List<Article> articles = getArticles();

		for (Article article : articles) {
			if (article.getId() == articleId) {
				return article;
			}
		}
		return null;
	}

	// 게시판 삭제
	public void deleteBoard(int id) {
		tables.get("board").delete(id);
	}

	public List<Article> getArticlesByBoardCode(String code) {
		Board board = getBoardByCode(code);
		// free => 2
		// notice => 1

		List<Article> articles = getArticles();
		List<Article> newArticles = new ArrayList<>();

		for (Article article : articles) {
			if (article.getBoardId() == board.getId()) {
				newArticles.add(article);
			}
		}

		return newArticles;
	}

	public Member getMemberByLoginIdAndLoginPw(String loginId, String loginPw) {
		List<Member> members = getMembers();

		for (Member member : members) {
			if (member.getLoginId().equals(loginId) && member.getLoginPw().equals(loginPw)) {
				return member;
			}
		}

		return null;
	}

	public Member getMemberByLoginId(String loginId) {
		List<Member> members = getMembers();

		for (Member member : members) {
			if (member.getLoginId().equals(loginId)) {
				return member;
			}
		}

		return null;
	}

	public List<Member> getMembers() {
		return tables.get("member").getRows();
	}

	// 일치하는 코드의 보드 리턴
	public Board getBoardByCode(String code) {
		List<Board> boards = getBoards();

		for (Board board : boards) {
			if (board.getCode().equals(code)) {
				return board;
			}
		}

		return null;
	}

	// code로 보드 갖고옴
	public Board getBoard(String code) {
		List<Board> boards = getBoards();

		for (Board board : boards) {
			if (board.getCode().equals(code)) {
				return board;
			}
		}

		return null;
	}

	// id로 보드 갖고옴
	public Board getBoard(int id) {
		return (Board) tables.get("board").getRow(id);
	}

	public List<Board> getBoards() {
		return tables.get("board").getRows();
	}

	public Member getMember(int id) {
		return (Member) tables.get("member").getRow(id);
	}

	public int saveBoard(Board board) {
		return tables.get("board").saveRow(board);
	}

	public String getDirPath() {
		return "db";
	}

	public int saveMember(Member member) {
		return tables.get("member").saveRow(member);
	}

	public List<Article> getArticles() {
		return tables.get("article").getRows();
	}

	public int saveArticle(Article article) {
		return tables.get("article").saveRow(article);
	}

	public void backup() {
		for (String tableName : tables.keySet()) {
			Table table = tables.get(tableName);
			table.backup();
		}
	}
}

// Table
class Table<T> {
	private Class<T> dataCls;
	private String tableName;
	private String tableDirPath;

	public Table(Class<T> dataCls, String dbDirPath) {
		this.dataCls = dataCls;
		this.tableName = Util.lcfirst(dataCls.getCanonicalName());
		this.tableDirPath = dbDirPath + "/" + this.tableName;

		Util.makeDir(tableDirPath);
	}

	// 수정
	public int modify(T data, int id) {
		Dto dto = (Dto) data;

		dto.setId(id);

		String rowFilePath = getRowFilePath(dto.getId());

		Util.writeJsonFile(rowFilePath, data);
		return dto.getId();
	}

	public int saveRow(T data) {
		Dto dto = (Dto) data;

		if (dto.getId() == 0) {
			int lastId = getLastId();
			int newId = lastId + 1;
			dto.setId(newId);
			setLastId(newId);
		}

		String rowFilePath = getRowFilePath(dto.getId());

		Util.writeJsonFile(rowFilePath, data);

		return dto.getId();
	};

	private String getRowFilePath(int id) {
		return tableDirPath + "/" + id + ".json";
	}

	private void setLastId(int lastId) {
		String filePath = getLastIdFilePath();
		Util.writeFileContents(filePath, lastId);
	}

	private int getLastId() {
		String filePath = getLastIdFilePath();

		if (Util.isFileExists(filePath) == false) {
			int lastId = 0;
			Util.writeFileContents(filePath, lastId);
			return lastId;
		}

		return Integer.parseInt(Util.getFileContents(filePath));
	}

	private String getLastIdFilePath() {
		return this.tableDirPath + "/lastId.txt";
	}

	public T getRow(int id) {
		return (T) Util.getObjectFromJson(getRowFilePath(id), dataCls);
	}

	public void backup() {

	}

	// 삭제
	void delete(int id) {
		String filePath = getRowFilePath(id);
		Util.deleteFileContents(filePath);
		// build site된 siteFolder내부의 파일을 같이 삭제해준다.
		Util.deleteFileContents("site/article/" + id + ".html");
	}

	List<T> getRows() {
		int lastId = getLastId();

		List<T> rows = new ArrayList<>();

		for (int id = 1; id <= lastId; id++) {
			T row = getRow(id);

			if (row != null) {
				rows.add(row);
			}
		}

		return rows;
	};
}

// DTO
abstract class Dto {
	private int id;
	private String regDate;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getRegDate() {
		return regDate;
	}

	public void setRegDate(String regDate) {
		this.regDate = regDate;
	}

	Dto() {
		this(0);
	}

	Dto(int id) {
		this(id, Util.getNowDateStr());
	}

	Dto(int id, String regDate) {
		this.id = id;
		this.regDate = regDate;
	}
}

class Board extends Dto {
	private String name;
	private String code;

	public Board() {
	}

	public Board(String name, String code) {
		this.name = name;
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

}

class Article extends Dto {
	private int boardId;
	private int memberId;
	private String title;
	private String body;

	public Article() {

	}

	public Article(int boardId, int memberId, String title, String body) {
		this.boardId = boardId;
		this.memberId = memberId;
		this.title = title;
		this.body = body;
	}

	public int getBoardId() {
		return boardId;
	}

	public void setBoardId(int boardId) {
		this.boardId = boardId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	@Override
	public String toString() {
		return "Article [boardId=" + boardId + ", memberId=" + memberId + ", title=" + title + ", body=" + body
				+ ", getId()=" + getId() + ", getRegDate()=" + getRegDate() + "]";
	}

}

class ArticleReply extends Dto {
	private int id;
	private String regDate;
	private int articleId;
	private int memberId;
	private String body;

	ArticleReply() {

	}

	public int getArticleId() {
		return articleId;
	}

	public void setArticleId(int articleId) {
		this.articleId = articleId;
	}

	public int getMemberId() {
		return memberId;
	}

	public void setMemberId(int memberId) {
		this.memberId = memberId;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

}

class Member extends Dto {
	private String loginId;
	private String loginPw;
	private String name;

	public Member() {

	}

	public Member(String loginId, String loginPw, String name) {
		this.loginId = loginId;
		this.loginPw = loginPw;
		this.name = name;
	}

	public String getLoginId() {
		return loginId;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public String getLoginPw() {
		return loginPw;
	}

	public void setLoginPw(String loginPw) {
		this.loginPw = loginPw;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}

// Util
class Util {
	// 현재날짜문장
	public static String getNowDateStr() {
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat Date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String dateStr = Date.format(cal.getTime());
		return dateStr;
	}

	// 파일에 내용쓰기
	public static void writeFileContents(String filePath, int data) {
		writeFileContents(filePath, data + "");
	}

	// 첫 문자 소문자화
	public static String lcfirst(String str) {
		String newStr = "";
		newStr += str.charAt(0);
		newStr = newStr.toLowerCase();

		return newStr + str.substring(1);
	}

	// 파일이 존재하는지
	public static boolean isFileExists(String filePath) {
		File f = new File(filePath);
		if (f.isFile()) {
			return true;
		}

		return false;
	}

	// 파일내용 읽어오기
	public static String getFileContents(String filePath) {
		String rs = null;
		try {
			// 바이트 단위로 파일읽기
			FileInputStream fileStream = null; // 파일 스트림

			fileStream = new FileInputStream(filePath);// 파일 스트림 생성
			// 버퍼 선언
			byte[] readBuffer = new byte[fileStream.available()];
			while (fileStream.read(readBuffer) != -1) {
			}

			rs = new String(readBuffer);

			fileStream.close(); // 스트림 닫기
		} catch (Exception e) {
			e.getStackTrace();
		}

		return rs;
	}

	// 삭제
	public static void deleteFileContents(String filePath) {
		File deleteFile = new File(filePath);
		deleteFile.delete();
	}

	// 파일 쓰기
	public static void writeFileContents(String filePath, String contents) {
		BufferedOutputStream bs = null;
		try {
			bs = new BufferedOutputStream(new FileOutputStream(filePath));
			bs.write(contents.getBytes()); // Byte형으로만 넣을 수 있음
		} catch (Exception e) {
			e.getStackTrace();
		} finally {
			try {
				bs.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	// Json안에 있는 내용을 가져오기
	public static Object getObjectFromJson(String filePath, Class cls) {
		ObjectMapper om = new ObjectMapper();
		Object obj = null;
		try {
			obj = om.readValue(new File(filePath), cls);
		} catch (JsonParseException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {

		} catch (IOException e) {
			e.printStackTrace();
		}

		return obj;
	}

	public static void writeJsonFile(String filePath, Object obj) {
		ObjectMapper om = new ObjectMapper();
		try {
			om.writeValue(new File(filePath), obj);
		} catch (JsonGenerationException e) {
			e.printStackTrace();
		} catch (JsonMappingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void makeDir(String dirPath) {
		File dir = new File(dirPath);
		if (!dir.exists()) {
			dir.mkdir();
		}
	}
}