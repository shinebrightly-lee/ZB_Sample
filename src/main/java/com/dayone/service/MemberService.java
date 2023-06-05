package com.dayone.service;

import com.dayone.exception.impl.AlreadyExistUserException;
import com.dayone.model.Auth;
import com.dayone.persist.repository.MemberRepository;
import com.dayone.persist.entity.MemberEntity;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MemberService implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return memberRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("couldn't find user -> " + username));
    }

    public MemberEntity register(Auth.SignUp member) {

        if (memberRepository.existsByUsername(member.getUsername())) {
            throw new AlreadyExistUserException();
        }

        // 비밀번호의 경우 DB 에 저장할 때 반드시 암호화를 해서 저장하도록 되어 있다.
        member.setPassword(passwordEncoder.encode(member.getPassword()));

        return memberRepository.save(member.toEntity());
    }

    // 비밀번호 인증 작업
    public MemberEntity authenticate(Auth.SignIn signIn) {

        MemberEntity user = memberRepository.findByUsername(signIn.getUsername())
                .orElseThrow(() -> new RuntimeException("존재하지 않는 ID 입니다."));

        if (!passwordEncoder.matches(signIn.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        return user;
    }
}