// 회원 엔티티 영속화를 담당하는 Repository.
package dev.backend.rabbitmq_notification.repository;

import dev.backend.rabbitmq_notification.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {
}
