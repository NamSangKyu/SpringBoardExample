package com.board;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

import com.board.dto.BoardCommentDTO;
import com.board.dto.BoardDTO;
import com.board.dto.LogDTO;
import com.board.dto.MemberDTO;
import com.board.mapper.BoardLogMapper;

@Component
@Aspect
public class BoardAOP {
	private PrintWriter pw;
	private BoardLogMapper mapper;
	public BoardAOP(BoardLogMapper mapper) {
		this.mapper = mapper;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy_MM_dd");
		String fileName = sdf.format(Calendar.getInstance().getTime())+"_log.csv";
		try {
			FileOutputStream fos = new FileOutputStream(fileName, true);
			pw = new PrintWriter(fos);
			System.out.println("로그 파일 연결 완료");
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	@Before("execution(* com.board.service.MemberService.*(..))")
	public void memberLog(JoinPoint joinpoint) {
		Object[] arr = joinpoint.getArgs();
		System.out.println(Arrays.toString(arr));
		//날짜시간,작업자,실행한메서드,작업데이터
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		StringBuffer str = new StringBuffer(); 
		str.append(sdf.format(Calendar.getInstance().getTime()) +",");
		str.append(joinpoint.getSignature().getName() + ",");
		
		for(Object obj : joinpoint.getArgs()) {
			if(obj instanceof MemberDTO) {
				MemberDTO dto = (MemberDTO) obj;
				str.append(dto.getId() + " / ");
				str.append(dto.getName() + " / ");
				str.append(dto.getGradeNo() + " / ");
				str.append(dto.getNick() + " / ");
				str.append(dto.getPasswd() + " / ");				
			}else if(obj instanceof HashMap) {
				Map map = (Map) obj;
				Iterator it = map.keySet().iterator();
				while(it.hasNext()) {
					str.append(it.next() + "/");
				}
			}else if(obj instanceof HttpSession){
				HttpSession session = (HttpSession) obj;
				MemberDTO dto = (MemberDTO) session.getAttribute("dto");
				str.append("," + dto.getId() + " / ");
			}else {
				str.append(obj + " / ");
			}
		}
		str.delete(str.length()-3, str.length()-1);
		pw.println(str);
		pw.flush();
		
	}
	
	//게시판에 글을 쓸 경우에 해당 글정보를 board_log에 저장
	// mapper를 새로 생성 -> Mapper : BoardLogMapper,  board_log_mapper.xml
	@Around("execution(* com.board.service.BoardService.insertBoard(..))")
	public Object insertBoardLog(ProceedingJoinPoint joinPoint) throws Throwable{
		Object val = joinPoint.proceed();
		System.out.println("게시판 글쓰기 기능 수행");
		BoardDTO dto = (BoardDTO)joinPoint.getArgs()[0];
		LogDTO log = new LogDTO();
		log.setRunMethod(joinPoint.getSignature().getName());
		log.setUpdateData(dto.toString());
		log.setWriter(dto.getWriter());
		mapper.insertLog(log);
		
		return val;
	}
	//게시판에 댓글 달았을때 로그 추가
	@Around("execution(* com.board.service.BoardService.insertBoardComment(..))")
	public Object insertBoardCommentLog(ProceedingJoinPoint joinPoint) throws Throwable {
		Object val = joinPoint.proceed();
		BoardCommentDTO dto = (BoardCommentDTO) joinPoint.getArgs()[0];
		LogDTO log = new LogDTO();
		log.setRunMethod(joinPoint.getSignature().getName());
		log.setUpdateData(dto.toString());
		log.setWriter(dto.getWriter());
		mapper.insertLog(log);
		
		return val;
	}
	//좋아요 싫어요 했을때도 로그 추가
	@Around("execution(* com.board.service.BoardService.insertBoard*(int, String))")
	public Object likeBoardLog(ProceedingJoinPoint joinPoint) throws Throwable {
		Object val = joinPoint.proceed();
		System.out.println(joinPoint.getSignature().getName());
		LogDTO log = new LogDTO();
		log.setRunMethod(joinPoint.getSignature().getName());
		log.setUpdateData(Arrays.toString(joinPoint.getArgs())+ 
				((int)val == 0 ? " / delete" : " / insert"));
		log.setWriter(joinPoint.getArgs()[1].toString());
		mapper.insertLog(log);
		return val;
	}
	//댓글 좋아요 싫어요 로그 추가
	@Around("execution(* com.board.service.BoardService.insertBoardCommentLikeHate(String, int, String))")
	public Object likeBoardCommentLog(ProceedingJoinPoint joinPoint) throws Throwable {
		Object val = joinPoint.proceed();
		System.out.println(joinPoint.getSignature().getName());
		LogDTO log = new LogDTO();
		log.setRunMethod(joinPoint.getSignature().getName());
		log.setUpdateData(Arrays.toString(joinPoint.getArgs())+ 
				((int)val == 0 ? " / delete" : " / insert"));
		log.setWriter(joinPoint.getArgs()[2].toString());
		mapper.insertLog(log);
		return val;
	}
	
	
	@Around("execution(* com.board.service.MemberService.login(..))")
	public Object loginLog(ProceedingJoinPoint joinPoint) throws Throwable{
		Object val = joinPoint.proceed();
		String str = "login : %s / %s ,접속주소 : %s, 요청시간 : %s, 결과 : %s";
		Object[] args = joinPoint.getArgs();
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = sdf.format(Calendar.getInstance().getTime());
		str = String.format(str, args[0],args[1],args[2], date, (val == null ? "로그인실패" : val.toString()));
		System.out.println(str);
		return val;
	}
	
	@Around("execution(* com.board.controller.MemberController.logout(..))")
	public Object logoutLog(ProceedingJoinPoint joinpoint) throws Throwable {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String date = sdf.format(Calendar.getInstance().getTime());
		HttpSession session = (HttpSession) joinpoint.getArgs()[0];
		MemberDTO dto = (MemberDTO) session.getAttribute("dto");
		System.out.println(dto.getId() + " 로그아웃, "+ date);
		return joinpoint.proceed();
	}
	
	
}










