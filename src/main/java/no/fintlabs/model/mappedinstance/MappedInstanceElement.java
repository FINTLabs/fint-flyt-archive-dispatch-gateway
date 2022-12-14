package no.fintlabs.model.mappedinstance;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@NoArgsConstructor
public class MappedInstanceElement {

    @Getter
    private String key;
    private Map<String, MappedInstanceElement> elementPerKey;
    private Map<String, MappedInstanceField> fieldPerKey;
    private Map<String, MappedInstanceCollectionField> collectionFieldPerKey;

    public MappedInstanceElement getElement(String key) {
        return elementPerKey.get(key);
    }

    public Optional<String> getFieldValue(String key) {
        return Optional.ofNullable(fieldPerKey.get(key))
                .map(MappedInstanceField::getValue);
    }

    public Optional<Collection<String>> getCollectionFieldValues(String key) {
        return Optional.ofNullable(collectionFieldPerKey.get(key))
                .map(MappedInstanceCollectionField::getValues);
    }

    public void setElements(Collection<MappedInstanceElement> elements) {
        this.elementPerKey = elements
                .stream()
                .collect(Collectors.toMap(
                        MappedInstanceElement::getKey,
                        Function.identity()
                ));
    }

    public void setFields(Collection<MappedInstanceField> fields) {
        this.fieldPerKey = fields
                .stream()
                .collect(Collectors.toMap(
                        MappedInstanceField::getKey,
                        Function.identity()
                ));
    }

    public void setCollectionFields(Collection<MappedInstanceCollectionField> collectionFields) {
        this.collectionFieldPerKey = collectionFields
                .stream()
                .collect(Collectors.toMap(
                        MappedInstanceCollectionField::getKey,
                        Function.identity()
                ));
    }

}
