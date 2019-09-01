package com.jandprocu.jandchase.api.productsms.service;

import com.jandprocu.jandchase.api.productsms.exception.ProductNotCreatedException;
import com.jandprocu.jandchase.api.productsms.exception.ProductNotFoundException;
import com.jandprocu.jandchase.api.productsms.exception.ProductNotUpdatedException;
import com.jandprocu.jandchase.api.productsms.repository.specification.ProductSpecification;
import com.jandprocu.jandchase.api.productsms.rest.ProductRequestByIds;
import com.jandprocu.jandchase.api.productsms.model.Product;
import com.jandprocu.jandchase.api.productsms.repository.ProductRepository;
import com.jandprocu.jandchase.api.productsms.rest.ProductResponse;
import com.jandprocu.jandchase.api.productsms.rest.ProductRest;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProductService implements IProductService {

    private ProductRepository productRepository;
    private ModelMapper modelMapper;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
        this.modelMapper = new ModelMapper();
        this.modelMapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
    }

    @Override
    public ProductResponse createProduct(ProductRest productRequest) {

        Product productEntity = modelMapper.map(productRequest, Product.class);
        productEntity.setProductId(UUID.randomUUID().toString());
        productEntity.setCreatedAt(new Date());
        try {
            productRepository.save(productEntity);
        } catch (DataAccessException exception) {
            throw new ProductNotCreatedException("Product " + productEntity.getName() + " not created");
        }

        return modelMapper.map(productEntity, ProductResponse.class);
    }

    @Override
    public ProductResponse getProductByProductId(String productId) {
        Product productEntity = getProductEntityByProductId(productId);
        return this.modelMapper.map(productEntity, ProductResponse.class);
    }

    @Override
    public ProductResponse updateProductByProductId(String productId, ProductRest updateRequest) {
        Product productEntity = getProductEntityByProductId(productId);
        Product updatedProduct = this.updateProductEntity(updateRequest, productEntity);
        try {
            productRepository.save(updatedProduct);
        } catch (DataAccessException exception) {
            throw new ProductNotUpdatedException("Product " + productId + " not updated");
        }
        return this.modelMapper.map(updatedProduct, ProductResponse.class);
    }

    @Override
    public ProductResponse partialUpdateProductByProductId(String productId, Map<String, Object> updateRequest) {
        Product productEntity = getProductEntityByProductId(productId);
        Product updatedProduct;
        try {
            updatedProduct = this.updatePartialProductEntity(updateRequest, productEntity);
            productRepository.save(updatedProduct);
        } catch (Exception exception) {
            throw new ProductNotUpdatedException("Product " + productId + " not updated");
        }
        return this.modelMapper.map(updatedProduct, ProductResponse.class);
    }

    private Product updatePartialProductEntity(Map<String, Object> updateRequest, Product productEntity) throws Exception {
        Class productClass = Product.class;

       boolean anyError = updateRequest.entrySet().stream().anyMatch(entry -> {
            boolean updated = true;
            String field = entry.getKey();
            try {
                Field productField = productClass.getDeclaredField(field);
                Class fieldType = productField.getType();
                if (fieldType.isPrimitive() ||  fieldType.isInstance(new String())) {
                    productEntity.setFieldValue(field,entry.getValue());
                }
            } catch (Exception e) {
                updated = false;
            }
            return updated == false;});

       if (anyError) {
           throw  new Exception();
       }
        return productEntity;
    }

    private Product updateProductEntity(ProductRest updateRequest, Product productEntity) {
        productEntity.setName(updateRequest.getName());
        productEntity.setDescription(updateRequest.getDescription());
        productEntity.setCategory(updateRequest.getCategory());
        productEntity.setAmount(updateRequest.getAmount());
        productEntity.setCurrency(updateRequest.getCurrency());
        return productEntity;
    }

    @Override
    public ProductResponse deleteProductByProductId(String productId) {
        Product productEntity = getProductEntityByProductId(productId);
        this.productRepository.deleteById(productEntity.getId());
        return this.modelMapper.map(productEntity, ProductResponse.class);
    }

    @Override
    public List<ProductResponse> getAllProductsByProductId(ProductRequestByIds requestByIds, int pageNo, int pageSize, String sortBy) {
        Pageable paging = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Page<Product> pagedResult = this.productRepository.findByProductIdIn(requestByIds.getProductIds(), paging);

        return getListOfProductsResponse(pagedResult);
    }

    @Override
    public List<ProductResponse> getAllProducts(String name, int pageNo, int pageSize, String sortBy) {
        Pageable paging = PageRequest.of(pageNo, pageSize, Sort.by(sortBy));
        Specification<Product> spec = Specification.where(new ProductSpecification(name));
        Page<Product> pagedResult = this.productRepository.findAll(spec, paging);

        return getListOfProductsResponse(pagedResult);
    }

    private List<ProductResponse> getListOfProductsResponse(Page<Product> pagedResult) {
        List<ProductResponse> productResponses = new ArrayList<>();

        if (pagedResult.hasContent()) {
            List<Product> products = pagedResult.getContent();
            productResponses.addAll(products.stream().map(product -> this.modelMapper.map(product, ProductResponse.class))
                    .collect(Collectors.toList()));
        }
        return productResponses;
    }

    private Product getProductEntityByProductId(String productId) {
        Product productEntity = this.productRepository.findByProductId(productId);
        if (productEntity == null)
            throw new ProductNotFoundException("Product with productId: " + productId + " not found");
        return productEntity;
    }
}
