package com.kuter.inventory;

import com.kuter.common.PageResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.inventory.dto.CreateStockAdjustmentDto;
import com.kuter.inventory.dto.CreateStockReceivingDto;
import com.kuter.inventory.dto.CurrentInventoryItemDto;
import com.kuter.inventory.dto.InventoryValuationReport;
import com.kuter.inventory.dto.StockAdjustmentResponse;
import com.kuter.inventory.dto.StockBalanceResponse;
import com.kuter.inventory.dto.StockMovementResponse;
import com.kuter.inventory.dto.StockReceivingResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InventoryService {

    StockReceivingResponse receiveStock(CreateStockReceivingDto dto,
                                        KuterUserPrincipal principal,
                                        HttpServletRequest request);

    PageResponse<StockReceivingResponse> listReceivings(Pageable pageable);

    StockReceivingResponse getReceiving(UUID id);

    StockAdjustmentResponse adjustStock(CreateStockAdjustmentDto dto,
                                        KuterUserPrincipal principal,
                                        HttpServletRequest request);

    PageResponse<StockAdjustmentResponse> listAdjustments(Pageable pageable);

    StockAdjustmentResponse getAdjustment(UUID id);

    PageResponse<StockBalanceResponse> listBalances(UUID categoryId,
                                                    Boolean lowStockOnly,
                                                    String search,
                                                    Pageable pageable);

    StockBalanceResponse getBalance(UUID productId);

    PageResponse<StockMovementResponse> listMovements(UUID productId,
                                                      MovementType movementType,
                                                      Pageable pageable);

    PageResponse<StockMovementResponse> listMovements(UUID productId,
                                                      MovementType movementType,
                                                      String referenceType,
                                                      UUID actorId,
                                                      java.time.Instant startDate,
                                                      java.time.Instant endDate,
                                                      String search,
                                                      Pageable pageable);

    InventoryValuationReport getValuationReport();

    PageResponse<CurrentInventoryItemDto> getDetailedValuationReport(UUID categoryId,
                                                                    String search,
                                                                    Pageable pageable);

    PageResponse<StockBalanceResponse> getLowStockReport(Pageable pageable);

    PageResponse<CurrentInventoryItemDto> getCurrentInventoryReport(UUID categoryId,
                                                                    String search,
                                                                    String stockStatus,
                                                                    Pageable pageable);
}
