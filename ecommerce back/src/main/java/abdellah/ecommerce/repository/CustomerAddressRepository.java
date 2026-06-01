package abdellah.ecommerce.repository;

import abdellah.ecommerce.domain.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    Optional<CustomerAddress> findFirstByCustomerProfile_IdAndDefaultAddressTrue(Long customerProfileId);
}
