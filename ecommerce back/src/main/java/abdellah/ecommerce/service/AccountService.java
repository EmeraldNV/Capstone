package abdellah.ecommerce.service;

import abdellah.ecommerce.api.dto.account.AddressResponse;
import abdellah.ecommerce.api.dto.account.UpdateProfileRequest;
import abdellah.ecommerce.api.dto.account.UserProfileResponse;
import abdellah.ecommerce.domain.entity.AppUser;
import abdellah.ecommerce.domain.entity.CustomerAddress;
import abdellah.ecommerce.domain.entity.CustomerProfile;
import abdellah.ecommerce.domain.enums.AddressType;
import abdellah.ecommerce.exception.NotFoundApiException;
import abdellah.ecommerce.repository.AppUserRepository;
import abdellah.ecommerce.repository.CustomerAddressRepository;
import abdellah.ecommerce.repository.CustomerProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AccountService {

    private final AppUserRepository appUserRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CustomerAddressRepository customerAddressRepository;

    public AccountService(AppUserRepository appUserRepository,
                          CustomerProfileRepository customerProfileRepository,
                          CustomerAddressRepository customerAddressRepository) {
        this.appUserRepository = appUserRepository;
        this.customerProfileRepository = customerProfileRepository;
        this.customerAddressRepository = customerAddressRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        CustomerProfile profile = loadOrNull(userId);
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundApiException("USER_NOT_FOUND", "User was not found."));
        CustomerAddress address = profile == null ? null : customerAddressRepository
                .findFirstByCustomerProfile_IdAndDefaultAddressTrue(profile.getId())
                .orElse(null);
        return toResponse(user, profile, address);
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        AppUser user = appUserRepository.findById(userId)
                .orElseThrow(() -> new NotFoundApiException("USER_NOT_FOUND", "User was not found."));
        CustomerProfile profile = customerProfileRepository.findByUser_Id(userId).orElseGet(() -> {
            CustomerProfile created = new CustomerProfile();
            created.setUser(user);
            return created;
        });

        profile.setFirstName(request.firstName().trim());
        profile.setLastName(request.lastName().trim());
        profile.setCompanyName(trimToNull(request.companyName()));
        profile.setTaxCode(trimToNull(request.taxCode()));
        profile.setVatNumber(trimToNull(request.vatNumber()));
        profile.setBirthDate(request.birthDate());
        profile.setPhone(trimToNull(request.phone()));
        profile.setMarketingConsent(Boolean.TRUE.equals(request.marketingConsent()));
        CustomerProfile savedProfile = customerProfileRepository.save(profile);

        CustomerAddress address = customerAddressRepository
                .findFirstByCustomerProfile_IdAndDefaultAddressTrue(savedProfile.getId())
                .orElseGet(() -> {
                    CustomerAddress created = new CustomerAddress();
                    created.setCustomerProfile(savedProfile);
                    created.setAddressType(AddressType.SHIPPING);
                    created.setDefaultAddress(Boolean.TRUE);
                    return created;
                });

        address.setLabel(request.address().label().trim());
        address.setRecipientName(request.address().recipientName().trim());
        address.setCompanyName(trimToNull(request.address().companyName()));
        address.setPhone(trimToNull(request.address().phone()));
        address.setLine1(request.address().line1().trim());
        address.setLine2(trimToNull(request.address().line2()));
        address.setCity(request.address().city().trim());
        address.setStateRegion(trimToNull(request.address().stateRegion()));
        address.setPostalCode(request.address().postalCode().trim());
        address.setCountryCode(request.address().countryCode().trim().toUpperCase());
        address.setDefaultAddress(Boolean.TRUE);
        customerAddressRepository.save(address);

        return toResponse(user, savedProfile, address);
    }

    private CustomerProfile loadOrNull(Long userId) {
        return customerProfileRepository.findWithDetailsByUser_Id(userId).orElse(null);
    }

    private UserProfileResponse toResponse(AppUser user, CustomerProfile profile, CustomerAddress address) {
        AddressResponse addressResponse = address == null ? null : new AddressResponse(
                address.getLabel(),
                address.getRecipientName(),
                address.getCompanyName(),
                address.getPhone(),
                address.getLine1(),
                address.getLine2(),
                address.getCity(),
                address.getStateRegion(),
                address.getPostalCode(),
                address.getCountryCode(),
                Boolean.TRUE.equals(address.getDefaultAddress())
        );

        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                Boolean.TRUE.equals(user.getEmailVerified()),
                user.getStatus().name(),
                profile == null ? null : profile.getFirstName(),
                profile == null ? null : profile.getLastName(),
                profile == null ? null : profile.getCompanyName(),
                profile == null ? null : profile.getTaxCode(),
                profile == null ? null : profile.getVatNumber(),
                profile == null ? null : profile.getBirthDate(),
                profile == null ? null : profile.getPhone(),
                profile != null && Boolean.TRUE.equals(profile.getMarketingConsent()),
                addressResponse,
                profile != null ? profile.getCreatedAt() : user.getCreatedAt(),
                profile != null ? profile.getUpdatedAt() : user.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
