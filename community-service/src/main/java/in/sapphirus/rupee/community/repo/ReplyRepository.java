package in.sapphirus.rupee.community.repo;

import in.sapphirus.rupee.community.domain.Reply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReplyRepository extends JpaRepository<Reply, UUID> {
    List<Reply> findByPostIdOrderByCreatedAtAsc(UUID postId);
}
