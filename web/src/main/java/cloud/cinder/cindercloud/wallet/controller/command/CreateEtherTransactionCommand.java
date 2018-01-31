package cloud.cinder.cindercloud.wallet.controller.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Min;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class CreateEtherTransactionCommand {

    @NotEmpty
    private String to;
    @Min(1)
    private long gasPrice;
    @Min(21000)
    private long gasLimit = 31000;
    private double amount;
}
