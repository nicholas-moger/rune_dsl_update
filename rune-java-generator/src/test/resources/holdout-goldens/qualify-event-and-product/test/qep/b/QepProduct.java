package test.qep.b;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.Accessor;
import com.rosetta.model.lib.annotations.AccessorType;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.qep.b.meta.QepProductMeta;

import static java.util.Optional.ofNullable;

/**
 * The product root, declared in the LATER namespace - first-wins is PER KIND.
 * @version 0.0.0
 */
@RosettaDataType(value="QepProduct", builder=QepProduct.QepProductBuilderImpl.class, version="0.0.0")
@RuneDataType(value="QepProduct", model="test", builder=QepProduct.QepProductBuilderImpl.class, version="0.0.0")
public interface QepProduct extends RosettaModelObject {

	QepProductMeta metaData = new QepProductMeta();

	/*********************** Getter Methods  ***********************/
	String getKind();

	/*********************** Build Methods  ***********************/
	QepProduct build();
	
	QepProduct.QepProductBuilder toBuilder();
	
	static QepProduct.QepProductBuilder builder() {
		return new QepProduct.QepProductBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends QepProduct> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends QepProduct> getType() {
		return QepProduct.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface QepProductBuilder extends QepProduct, RosettaModelObjectBuilder {
		QepProduct.QepProductBuilder setKind(String kind);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("kind"), String.class, getKind(), this);
		}
		

		QepProduct.QepProductBuilder prune();
	}

	/*********************** Immutable Implementation of QepProduct  ***********************/
	class QepProductImpl implements QepProduct {
		private final String kind;
		
		protected QepProductImpl(QepProduct.QepProductBuilder builder) {
			this.kind = builder.getKind();
		}
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public String getKind() {
			return kind;
		}
		
		@Override
		public QepProduct build() {
			return this;
		}
		
		@Override
		public QepProduct.QepProductBuilder toBuilder() {
			QepProduct.QepProductBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(QepProduct.QepProductBuilder builder) {
			ofNullable(getKind()).ifPresent(builder::setKind);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			QepProduct _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "QepProduct {" +
				"kind=" + this.kind +
			'}';
		}
	}

	/*********************** Builder Implementation of QepProduct  ***********************/
	class QepProductBuilderImpl implements QepProduct.QepProductBuilder {
	
		protected String kind;
		
		@Override
		@RosettaAttribute("kind")
		@Accessor(AccessorType.GETTER)
		@RuneAttribute("kind")
		public String getKind() {
			return kind;
		}
		
		@RosettaAttribute("kind")
		@Accessor(AccessorType.SETTER)
		@RuneAttribute("kind")
		@Override
		public QepProduct.QepProductBuilder setKind(String _kind) {
			this.kind = _kind == null ? null : _kind;
			return this;
		}
		
		@Override
		public QepProduct build() {
			return new QepProduct.QepProductImpl(this);
		}
		
		@Override
		public QepProduct.QepProductBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public QepProduct.QepProductBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getKind()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public QepProduct.QepProductBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			QepProduct.QepProductBuilder o = (QepProduct.QepProductBuilder) other;
			
			
			merger.mergeBasic(getKind(), o.getKind(), this::setKind);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			QepProduct _that = getType().cast(o);
		
			if (!Objects.equals(kind, _that.getKind())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (kind != null ? kind.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "QepProductBuilder {" +
				"kind=" + this.kind +
			'}';
		}
	}
}
