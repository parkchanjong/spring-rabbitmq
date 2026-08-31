// 회원 도메인 엔티티.
package dev.backend.rabbitmq_notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "members")
public class Member {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private String name;

	@Column(nullable = false, updatable = false)
	private LocalDateTime createdAt;

	protected Member() {
	}

	private Member(String name) {
		this.name = name;
		this.createdAt = LocalDateTime.now();
	}

	public static Member create(String name) {
		return new Member(name);
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}
}
