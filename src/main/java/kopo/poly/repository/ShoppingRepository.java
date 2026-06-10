package kopo.poly.repository;

import kopo.poly.repository.entity.ShoppingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ShoppingRepository extends JpaRepository<ShoppingEntity, Integer> {

    /** 사용자 장바구니 전체 조회 (최신순) */
    List<ShoppingEntity> findByUserIdOrderByRegDtDesc(Integer userId);

    /** 소유권 검증 포함 단건 조회 — 타인의 항목 삭제 방지 */
    Optional<ShoppingEntity> findByCartIdAndUserId(Integer cartId, Integer userId);

    /** 장바구니 전체 삭제 */
    @Modifying
    @Query("DELETE FROM ShoppingEntity s WHERE s.userId = :userId")
    void deleteAllByUserId(@Param("userId") Integer userId);

    /** 마이페이지 통계 — 유저의 장바구니 담긴 수 */
    long countByUserId(Integer userId);
}
